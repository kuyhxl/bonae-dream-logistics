package com.bonae.gateway.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String header,
        String prefix
) {

    // HS256 서명 키 최소 길이
    private static final int MIN_KEY_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 설정이 필요합니다. JWT_SECRET 환경변수를 확인하세요.");
        }

        // BASE64
        byte[] decoded;
        try{
            decoded = Decoders.BASE64.decode(secret);
        }
        catch(Exception e){
            throw new IllegalArgumentException("jwt.secret은 BASE64로 인코딩된 값이어야합니다.");
        }

        if (decoded.length < MIN_KEY_BYTES){
            throw new IllegalArgumentException("jwt.secret은 BASE64 디코딩 후 최소 " + MIN_KEY_BYTES + "바이트여야 합니다.");
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