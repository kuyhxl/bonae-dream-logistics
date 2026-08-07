package com.bonae.logistics.user.infrastructure.security;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
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
}
