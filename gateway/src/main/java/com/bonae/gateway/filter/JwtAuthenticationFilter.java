package com.bonae.gateway.filter;

import com.bonae.gateway.config.AuthPathProperties;
import com.bonae.gateway.config.JwtProperties;
import com.bonae.gateway.jwt.InvalidTokenException;
import com.bonae.gateway.jwt.JwtValidator;
import com.bonae.gateway.jwt.TokenBlacklistChecker;
import com.bonae.gateway.jwt.TokenClaims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String USER_HUB_ID_HEADER = "X-User-Hub-Id";
    public static final String USER_COMPANY_ID_HEADER = "X-User-Company-Id";

    private final JwtValidator jwtValidator;
    private final JwtProperties jwtProperties;
    private final AuthPathProperties authPathProperties;
    private final AuthenticationErrorWriter errorWriter;
    private final TokenBlacklistChecker blacklistChecker;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtValidator jwtValidator,
                                   JwtProperties jwtProperties,
                                   AuthPathProperties authPathProperties,
                                   AuthenticationErrorWriter errorWriter,
                                   TokenBlacklistChecker blacklistChecker) {
        this.jwtValidator = jwtValidator;
        this.jwtProperties = jwtProperties;
        this.authPathProperties = authPathProperties;
        this.errorWriter = errorWriter;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (isPermitAll(path)) {
            // 인증 제외 경로라도 클라이언트가 보낸 사용자 헤더는 신뢰x
            return chain.filter(stripUserHeaders(exchange));
        }

        String token = jwtProperties.resolveToken(request.getHeaders().getFirst(jwtProperties.header()));
        if (token == null) {
            log.warn("인증 실패: 토큰 없음 - {} {}", request.getMethod(), path);
            return errorWriter.unauthorized(exchange);
        }

        TokenClaims claims;
        try {
            claims = jwtValidator.validate(token);
        } catch (InvalidTokenException e) {
            // 사유는 로그에만
            log.warn("인증 실패: {} - {} {}", e.getReason(), request.getMethod(), path);
            return errorWriter.unauthorized(exchange);
        }

        TokenClaims validated = claims;

        // 서명, 만료 검증을 통과한 토큰만 조회
        // onErrorResume은 조회 구간에만 걸어서 이후 라우팅 단계의 오류까지 삼키지 않도록
        return blacklistChecker.isBlacklisted(claims.jti())
                .onErrorResume(e -> {
                    log.error("블랙리스트 조회 실패 - {} {}", request.getMethod(), path, e);
                    return Mono.error(new BlacklistUnavailableException(e));
                })
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("인증 실패: 로그아웃된 토큰 - {} {}", request.getMethod(), path);
                        return errorWriter.unauthorized(exchange);
                    }
                    return chain.filter(withUserHeaders(exchange, validated));
                })
                .onErrorResume(BlacklistUnavailableException.class,
                        e -> errorWriter.serviceUnavailable(exchange));
    }

    // 블랙리스트 조회 실패를 라우팅 단계의 오류랑 구별하기 위한 내부 신호입미다
    private static class BlacklistUnavailableException extends RuntimeException {
        BlacklistUnavailableException(Throwable cause) {
            super(cause);
        }
    }

    private boolean isPermitAll(String path) {
        return authPathProperties.permitAll().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // 클라이언트가 위조해 보낸 사용자 헤더를 모두 제거한다
    private ServerWebExchange stripUserHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(builder -> builder.headers(this::removeAllUserHeaders))
                .build();
    }

    // 검증된 토큰의 값으로 사용자 헤더를 덮어씀. 없는 값은 헤더를 제거
    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, TokenClaims claims) {
        return exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.set(USER_ID_HEADER, claims.username());
                    headers.set(USER_ROLE_HEADER, claims.role());
                    // 소속이 없는 역할(MASTER 등)은 헤더를 넣지 않되,
                    // 클라이언트가 보낸 값이 통과하지 않도록 반드시 제거한다.
                    setOrRemove(headers, USER_HUB_ID_HEADER, claims.hubId());
                    setOrRemove(headers, USER_COMPANY_ID_HEADER, claims.companyId());
                }))
                .build();
    }

    private void setOrRemove(HttpHeaders headers, String name, String value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.set(name, value);
        }
    }

    private void removeAllUserHeaders(HttpHeaders headers) {
        headers.remove(USER_ID_HEADER);
        headers.remove(USER_ROLE_HEADER);
        headers.remove(USER_HUB_ID_HEADER);
        headers.remove(USER_COMPANY_ID_HEADER);
    }

    @Override
    public int getOrder() {
        // 라우팅 필터보다 먼저 실행되어야 함
        return -100;
    }
}