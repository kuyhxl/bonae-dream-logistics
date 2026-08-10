package com.bonae.gateway.filter;

import com.bonae.gateway.config.AuthPathProperties;
import com.bonae.gateway.config.JwtProperties;
import com.bonae.gateway.jwt.JwtValidator;
import com.bonae.gateway.jwt.TokenBlacklistChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("JwtAuthenticationFilter 인증 및 헤더 주입")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1nYXRld2F5ISE=";
    private static final String USERNAME = "bonaedreamida";
    private static final String ROLE = "COMPANY_MANAGER";
    private static final String HUB_ID = "3b1c3a78-2b73-4501-bf16-feec87fc98c4";
    private static final String COMPANY_ID = "06950820-c85b-4669-badb-c623011d6ad1";

    private JwtAuthenticationFilter filter;
    private TokenBlacklistChecker blacklistChecker;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(SECRET, "Authorization", "Bearer ");
        AuthPathProperties authPaths = new AuthPathProperties(List.of(
                "/api/auth/signup", "/api/auth/login", "/api/auth/refresh", "/v3/api-docs/**"));

        // 테스트에서 필요할 때 스텁을 덮어씀
        blacklistChecker = mock(TokenBlacklistChecker.class);
        given(blacklistChecker.isBlacklisted(anyString())).willReturn(Mono.just(false));

        filter = new JwtAuthenticationFilter(
                new JwtValidator(jwtProperties),
                jwtProperties,
                authPaths,
                new AuthenticationErrorWriter(new ObjectMapper(), mock(Tracer.class)),
                blacklistChecker);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    // hubId/companyId에 null을 넘기면 해당 클레임을 생략한다
    private String accessToken(String role, String hubId, String companyId) {
        var builder = Jwts.builder()
                .subject(USERNAME)
                .claim("role", role)
                .claim("typ", "ACCESS")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)));

        if (hubId != null) builder.claim("hubId", hubId);
        if (companyId != null) builder.claim("companyId", companyId);

        return builder.signWith(key()).compact();
    }

    // 체인으로 넘어간 exchange를 반환한다. 인증에 실패하면 null
    private ServerWebExchange forwardedFor(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            captured.set(ex);
            return Mono.empty();
        }).block();
        return captured.get();
    }

    private HttpStatus statusOf(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, ex -> Mono.empty()).block();
        return HttpStatus.valueOf(exchange.getResponse().getStatusCode().value());
    }

    @Nested
    @DisplayName("인증 성공")
    class Success {

        @Test
        @DisplayName("유효한 토큰이면 사용자 헤더를 주입해 전달한다")
        void injectsUserHeaders() {
            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.get("/api/companies")
                            .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                            .build());

            var headers = forwarded.getRequest().getHeaders();
            assertThat(headers.getFirst("X-User-Id")).isEqualTo(USERNAME);
            assertThat(headers.getFirst("X-User-Role")).isEqualTo(ROLE);
            assertThat(headers.getFirst("X-User-Hub-Id")).isEqualTo(HUB_ID);
            assertThat(headers.getFirst("X-User-Company-Id")).isEqualTo(COMPANY_ID);
        }

        @Test
        @DisplayName("소속이 없는 역할이면 소속 헤더를 넣지 않는다")
        void omitsAffiliationHeadersWhenAbsent() {
            // MASTER는 hubId/companyId가 없다.
            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.get("/api/companies")
                            .header("Authorization", "Bearer " + accessToken("MASTER", null, null))
                            .build());

            var headers = forwarded.getRequest().getHeaders();
            assertThat(headers.getFirst("X-User-Role")).isEqualTo("MASTER");
            assertThat(headers.getFirst("X-User-Hub-Id")).isNull();
            assertThat(headers.getFirst("X-User-Company-Id")).isNull();
        }

        @Test
        @DisplayName("클라이언트가 보낸 사용자 헤더는 토큰 값으로 덮어쓴다")
        void overwritesForgedHeaders() {
            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.get("/api/companies")
                            .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                            .header("X-User-Id", "attacker")
                            .header("X-User-Role", "MASTER")
                            .build());

            var headers = forwarded.getRequest().getHeaders();
            assertThat(headers.get("X-User-Id")).containsExactly(USERNAME);
            assertThat(headers.get("X-User-Role")).containsExactly(ROLE);
        }

        @Test
        @DisplayName("토큰에 소속이 없으면 클라이언트가 보낸 소속 헤더를 제거한다")
        void removesForgedAffiliationHeaders() {
            // 소속이 없는 MASTER가 남의 업체 ID를 직접 넣어 보내는 경우
            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.get("/api/companies")
                            .header("Authorization", "Bearer " + accessToken("MASTER", null, null))
                            .header("X-User-Company-Id", "11111111-1111-1111-1111-111111111111")
                            .header("X-User-Hub-Id", "22222222-2222-2222-2222-222222222222")
                            .build());

            var headers = forwarded.getRequest().getHeaders();
            assertThat(headers.getFirst("X-User-Company-Id")).isNull();
            assertThat(headers.getFirst("X-User-Hub-Id")).isNull();
        }
    }

    @Nested
    @DisplayName("인증 실패")
    class Failure {

        @Test
        @DisplayName("토큰이 없으면 401을 반환한다")
        void noToken() {
            assertThat(statusOf(MockServerHttpRequest.get("/api/companies").build()))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Bearer 접두사가 없으면 401을 반환한다")
        void missingBearerPrefix() {
            assertThat(statusOf(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", accessToken(ROLE, HUB_ID, COMPANY_ID))
                    .build()))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("형식이 잘못된 토큰이면 401을 반환한다")
        void malformedToken() {
            assertThat(statusOf(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer not-a-jwt")
                    .build()))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("인증에 실패하면 체인으로 전달하지 않는다")
        void doesNotForward() {
            assertThat(forwardedFor(MockServerHttpRequest.get("/api/companies").build())).isNull();
        }
    }

    @Nested
    @DisplayName("인증 제외 경로")
    class PermitAll {

        @Test
        @DisplayName("로그인 경로는 토큰 없이 통과한다")
        void loginPasses() {
            assertThat(forwardedFor(MockServerHttpRequest.post("/api/auth/login").build())).isNotNull();
        }

        @Test
        @DisplayName("토큰 재발급 경로는 토큰 없이 통과한다")
        void refreshPasses() {
            assertThat(forwardedFor(MockServerHttpRequest.post("/api/auth/refresh").build())).isNotNull();
        }

        @Test
        @DisplayName("Swagger 경로는 토큰 없이 통과한다")
        void swaggerPasses() {
            assertThat(forwardedFor(MockServerHttpRequest.get("/v3/api-docs/swagger-config").build()))
                    .isNotNull();
        }

        @Test
        @DisplayName("제외 경로여도 클라이언트가 보낸 사용자 헤더는 제거한다")
        void stripsForgedHeadersOnPermitAll() {
            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.post("/api/auth/login")
                            .header("X-User-Role", "MASTER")
                            .header("X-User-Company-Id", "11111111-1111-1111-1111-111111111111")
                            .build());

            var headers = forwarded.getRequest().getHeaders();
            assertThat(headers.getFirst("X-User-Role")).isNull();
            assertThat(headers.getFirst("X-User-Company-Id")).isNull();
        }

        @Test
        @DisplayName("로그아웃은 제외 경로가 아니므로 토큰이 필요하다")
        void logoutRequiresToken() {
            // jti를 추출해 블랙리스트에 등록해야 하므로 인증을 거쳐야 함
            assertThat(statusOf(MockServerHttpRequest.post("/api/auth/logout").build()))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("제외 경로는 Redis를 조회하지 않는다")
        void doesNotQueryRedis() {
            // 레디스 장애 중에도 로그인은 가능해야 복구할 수 있다
            forwardedFor(MockServerHttpRequest.post("/api/auth/login").build());

            verify(blacklistChecker, never()).isBlacklisted(anyString());
        }
    }

    @Nested
    @DisplayName("블랙리스트")
    class Blacklist {

        @Test
        @DisplayName("블랙리스트에 없는 토큰은 통과한다")
        void notBlacklistedPasses() {
            given(blacklistChecker.isBlacklisted(anyString())).willReturn(Mono.just(false));

            ServerWebExchange forwarded = forwardedFor(
                    MockServerHttpRequest.get("/api/companies")
                            .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                            .build());

            assertThat(forwarded).isNotNull();
            assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("로그아웃된 토큰은 401을 반환한다")
        void blacklistedRejected() {
            given(blacklistChecker.isBlacklisted(anyString())).willReturn(Mono.just(true));

            assertThat(statusOf(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                    .build()))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("로그아웃된 토큰은 체인으로 전달하지 않는다")
        void blacklistedNotForwarded() {
            given(blacklistChecker.isBlacklisted(anyString())).willReturn(Mono.just(true));

            assertThat(forwardedFor(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                    .build()))
                    .isNull();
        }

        @Test
        @DisplayName("Redis 조회에 실패하면 503을 반환한다")
        void redisFailureReturnsServiceUnavailable() {
            // 블랙리스트는 보안 통제이므로 확인할 수 없으면 통과시키지 않음
            given(blacklistChecker.isBlacklisted(anyString()))
                    .willReturn(Mono.error(new RuntimeException("redis down")));

            assertThat(statusOf(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                    .build()))
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("Redis 조회에 실패하면 체인으로 전달하지 않는다")
        void redisFailureNotForwarded() {
            given(blacklistChecker.isBlacklisted(anyString()))
                    .willReturn(Mono.error(new RuntimeException("redis down")));

            assertThat(forwardedFor(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer " + accessToken(ROLE, HUB_ID, COMPANY_ID))
                    .build()))
                    .isNull();
        }

        @Test
        @DisplayName("유효하지 않은 토큰은 Redis를 조회하지 않는다")
        void invalidTokenSkipsRedis() {
            statusOf(MockServerHttpRequest.get("/api/companies")
                    .header("Authorization", "Bearer not-a-jwt")
                    .build());

            verify(blacklistChecker, never()).isBlacklisted(anyString());
        }
    }
}