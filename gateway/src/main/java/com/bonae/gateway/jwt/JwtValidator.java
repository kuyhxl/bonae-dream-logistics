package com.bonae.gateway.jwt;

import com.bonae.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import static com.bonae.gateway.jwt.InvalidTokenException.Reason;


@Component
public class JwtValidator {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String CLAIM_COMPANY_ID = "companyId";
    private static final String CLAIM_HUB_ID = "hubId";

    private final SecretKey secretKey;

    public JwtValidator(JwtProperties jwtProperties) {
        // BASE64 디코딩한 바이트로 HS256키 만들기
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public TokenClaims validate(String token) {
        Claims claims = parse(token);

        requireAccessToken(claims);

        String username = require(claims.getSubject(), "sub");
        String jti = require(claims.getId(), "jti");
        String role = requireAllowedRole(claims.get(CLAIM_ROLE, String.class));

        String hubId = optional(claims.get(CLAIM_HUB_ID, String.class));
        String companyId = optional(claims.get(CLAIM_COMPANY_ID, String.class));

        return new TokenClaims(username, role, jti, hubId, companyId);
    }

    private Claims parse(String token){
        try{
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
        catch (ExpiredJwtException e){
            throw new InvalidTokenException(Reason.EXPIRED, "만료된 토큰입니다. ");
        }
        catch (SignatureException e){
            throw new InvalidTokenException(Reason.SIGNATURE_INVALID, "서명이 유효하지 않은 토큰 입니다. ");
        }
        catch (JwtException | IllegalArgumentException e){
            throw new InvalidTokenException(Reason.MALFORMED, "형식이 올바르지 않은 토큰입니다.");
        }
    }

    // 리프레시 토큰으로 API 접근 차단
    private void requireAccessToken(Claims claims){
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!ACCESS_TOKEN_TYPE.equals(type)){
            throw new InvalidTokenException(Reason.NOT_ACCESS_TOKEN, "엑세스 토큰이 아닙니다.");
        }
    }

    private String require(String value, String claimName){
        if(value == null || value.isBlank()){
            throw new InvalidTokenException(Reason.CLAIM_MISSING, claimName + "클레임이 없습니다.");
        }
        return value;
    }

    private String requireAllowedRole(String role){
        require(role, CLAIM_ROLE);
        try{
            return UserRole.valueOf(role).name();
        }
        catch (IllegalArgumentException e) {
            throw new InvalidTokenException(Reason.ROLE_NOT_ALLOWED, "허용되지 않은 권한입니다." + role);
        }
    }

    private String optional(String value){
        return (value == null || value.isBlank()) ? null : value;
    }
}
