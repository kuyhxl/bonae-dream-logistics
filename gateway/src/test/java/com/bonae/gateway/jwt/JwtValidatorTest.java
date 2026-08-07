package com.bonae.gateway.jwt;

import com.bonae.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtValidator 액세스 토큰 검증")
class JwtValidatorTest {

    // BASE64 인코딩된 32바이트 키 (테스트 전용)
    private static final String SECRET =
            "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1nYXRld2F5ISE=";
    private static final String OTHER_SECRET =
            "YW5vdGhlci1zZWNyZXQta2V5LXVzZWQtZm9yLWZvcmdlcnkhIQ==";

    private static final String USERNAME = "bonaedreamida";
    private static final String ROLE = "COMPANY_MANAGER";

    private JwtValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JwtValidator(new JwtProperties(SECRET, "Authorization", "Bearer "));
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // 클레임을 개별 지정해 토큰을 만든다. null을 넘기면 해당 클레임을 생략한다.
    private String token(String secret, String sub, String role, String typ, String jti, Instant exp) {
        var builder = Jwts.builder()
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(exp));

        if (sub != null) builder.subject(sub);
        if (role != null) builder.claim("role", role);
        if (typ != null) builder.claim("typ", typ);
        if (jti != null) builder.id(jti);

        return builder.signWith(key(secret)).compact();
    }

    private String validToken() {
        return token(SECRET, USERNAME, ROLE, "ACCESS",
                UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));
    }

    private void assertRejected(String token, InvalidTokenException.Reason reason) {
        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(reason);
    }

    @Test
    @DisplayName("정상 액세스 토큰은 클레임을 추출한다")
    void validAccessToken() {
        String jti = UUID.randomUUID().toString();
        String token = token(SECRET, USERNAME, ROLE, "ACCESS", jti, Instant.now().plusSeconds(3600));

        TokenClaims claims = validator.validate(token);

        assertThat(claims.username()).isEqualTo(USERNAME);
        assertThat(claims.role()).isEqualTo(ROLE);
        assertThat(claims.jti()).isEqualTo(jti);
    }

    @Nested
    @DisplayName("서명 / 형식")
    class SignatureAndFormat {

        @Test
        @DisplayName("다른 시크릿으로 서명한 토큰은 거부한다")
        void forgedSignature() {
            String token = token(OTHER_SECRET, USERNAME, ROLE, "ACCESS",
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.SIGNATURE_INVALID);
        }

        @Test
        @DisplayName("형식이 잘못된 문자열은 거부한다")
        void malformed() {
            assertRejected("not-a-jwt-token", InvalidTokenException.Reason.MALFORMED);
        }

        @Test
        @DisplayName("빈 문자열은 거부한다")
        void blank() {
            assertRejected("", InvalidTokenException.Reason.MALFORMED);
        }
    }

    @Nested
    @DisplayName("만료 / 토큰 타입")
    class ExpiryAndType {

        @Test
        @DisplayName("만료된 토큰은 거부한다")
        void expired() {
            String token = token(SECRET, USERNAME, ROLE, "ACCESS",
                    UUID.randomUUID().toString(), Instant.now().minusSeconds(60));

            assertRejected(token, InvalidTokenException.Reason.EXPIRED);
        }

        @Test
        @DisplayName("리프레시 토큰으로는 접근할 수 없다")
        void refreshToken() {
            // 리프레시 토큰은 typ=REFRESH이며 role 클레임이 없다.
            String token = token(SECRET, USERNAME, null, "REFRESH",
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(604800));

            assertRejected(token, InvalidTokenException.Reason.NOT_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("typ 클레임이 없으면 거부한다")
        void missingType() {
            String token = token(SECRET, USERNAME, ROLE, null,
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.NOT_ACCESS_TOKEN);
        }
    }

    @Nested
    @DisplayName("필수 클레임")
    class RequiredClaims {

        @Test
        @DisplayName("sub가 없으면 거부한다")
        void missingSubject() {
            String token = token(SECRET, null, ROLE, "ACCESS",
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.CLAIM_MISSING);
        }

        @Test
        @DisplayName("jti가 없으면 거부한다")
        void missingJti() {
            // jti가 없으면 로그아웃 블랙리스트 조회가 불가능하다.
            String token = token(SECRET, USERNAME, ROLE, "ACCESS", null,
                    Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.CLAIM_MISSING);
        }

        @Test
        @DisplayName("role이 없으면 거부한다")
        void missingRole() {
            // 미승인(PENDING) 사용자는 role이 null이다.
            String token = token(SECRET, USERNAME, null, "ACCESS",
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.CLAIM_MISSING);
        }

        @Test
        @DisplayName("허용되지 않은 role 값은 거부한다")
        void unknownRole() {
            String token = token(SECRET, USERNAME, "SUPER_ADMIN", "ACCESS",
                    UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

            assertRejected(token, InvalidTokenException.Reason.ROLE_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("허용된 role 값")
    class AllowedRoles {

        @Test
        @DisplayName("확정된 4개 권한을 모두 통과시킨다")
        void allFourRoles() {
            for (UserRole role : UserRole.values()) {
                String token = token(SECRET, USERNAME, role.name(), "ACCESS",
                        UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

                assertThat(validator.validate(token).role()).isEqualTo(role.name());
            }
        }
    }
}