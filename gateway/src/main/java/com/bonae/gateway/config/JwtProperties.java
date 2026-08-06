package com.bonae.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String header,
        String prefix
) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 설정이 필요합니다. JWT_SECRET 환경변수를 확인하세요.");
        }

        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret은 최소 32바이트 이상이어야 합니다.");
        }
    }

    public String resolveToken(String headerValue) {
        if (headerValue == null || !headerValue.startsWith(prefix)) {
            return null;
        }
        String token = headerValue.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }
}