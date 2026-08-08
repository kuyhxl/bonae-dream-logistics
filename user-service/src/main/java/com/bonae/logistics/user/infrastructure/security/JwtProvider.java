package com.bonae.logistics.user.infrastructure.security;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    public static final String TOKEN_TYPE = "Bearer";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    public String createAccessToken(String username, Role role) {
        return createToken(username, role, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
    }

    public String createRefreshToken(String username) {
        return createToken(username, null, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiration());
    }

    private String createToken(String username, Role role, String type, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        JwtBuilder builder = Jwts.builder()
                .subject(username) // sub
                .claim(CLAIM_TYPE, type) // typ
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(now) // iat
                .expiration(expiration); // exp

        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }

        return builder.signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    /* refreshToken을 검증하고 claims를 반환 */
    public Claims parseRefreshToken(String token) {
        Claims claims;

        try {
            claims = parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // refreshToken 자리에 accessToken이 들어오는 경우 차단
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return claims;
    }

    /* 블랙리스트용 accessToken 파싱 예외를 던지지 않고 만료·위조된 토큰은 비어있는 값으로 반환 */
    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /* 토큰 만료까지 남은 시간(ms). 이미 만료됐으면 0 이하 */
    public long remainingMillis(Claims claims) {
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }


    // secretKey로 JWT의 위변조 여부를 확인한 뒤 페이로드 정보를 가져옴.
    private  Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey) // 검증할 secretKey
                .build()
                .parseSignedClaims(token) // 문자열 형태의 JWT를 파싱하고 서명을 검증, 형식이 잘못되었거나, 서명이 틀렸거나, 만료된 토큰이면 예외가 발생
                .getPayload(); // 검증된 JWT의 Claims를 반환 - 사용자 ID, 권한, 발급 시간(iat), 만료 시간(exp)
    }
}
