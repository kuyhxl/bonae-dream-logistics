package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import com.bonae.logistics.user.infrastructure.persistence.RefreshTokenStore;
import com.bonae.logistics.user.infrastructure.security.JwtProvider;
import com.bonae.logistics.user.presentation.dto.request.RefreshRequest;
import com.bonae.logistics.user.presentation.dto.response.TokenResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 토큰 재발급")
class AuthServiceRefreshTest {

    private static final String USERNAME = "testuser";
    private static final String OLD_REFRESH_TOKEN = "old.refresh.token";
    private static final String NEW_REFRESH_TOKEN = "new.refresh.token";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";
    private static final long ACCESS_EXP = 3600000L;
    private static final long REFRESH_EXP = 604800000L;

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private Claims claims;

    @BeforeEach
    void setUp() {
        given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willReturn(claims);
        given(claims.getSubject()).willReturn(USERNAME);
    }

    private User approvedUser() {
        return User.builder()
                .username(USERNAME)
                .password("encoded")
                .name("테스트")
                .slackId("U000TEST123")
                .affiliationName("테스트업체")
                .role(Role.COMPANY_MANAGER)
                .status(Status.APPROVED)
                .build();
    }

    @Test
    @DisplayName("유효한 refreshToken이면 accessToken과 refreshToken을 모두 새로 발급한다")
    void refresh_success() {
        // given
        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.of(OLD_REFRESH_TOKEN));
        given(userRepository.findByUsernameAndDeletedAtIsNull(USERNAME)).willReturn(Optional.of(approvedUser()));
        given(jwtProvider.createAccessToken(anyString(), any(), any(), any())).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.createRefreshToken(USERNAME)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProperties.getAccessTokenExpiration()).willReturn(ACCESS_EXP);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(REFRESH_EXP);

        // when
        TokenResponse response = authService.refresh(new RefreshRequest(OLD_REFRESH_TOKEN));

        // then
        assertThat(response.getAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.getTokenType()).isEqualTo(JwtProvider.TOKEN_TYPE);
        assertThat(response.getExpiresIn()).isEqualTo(ACCESS_EXP / 1000);
        assertThat(response.getRefreshExpiresIn()).isEqualTo(REFRESH_EXP / 1000);

        then(refreshTokenStore).should().save(USERNAME, NEW_REFRESH_TOKEN, REFRESH_EXP);
    }

    @Test
    @DisplayName("저장소에 refreshToken이 없으면 REFRESH_TOKEN_NOT_FOUND")
    void refresh_notFoundInStore() {
        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        then(refreshTokenStore).should(never()).save(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("이미 회전된 옛 refreshToken이 재사용되면 저장된 토큰까지 폐기한다")
    void refresh_reuseDetected() {
        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.of(NEW_REFRESH_TOKEN));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        then(refreshTokenStore).should().delete(USERNAME);
        then(refreshTokenStore).should(never()).save(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("승인 상태가 아닌 사용자는 재발급할 수 없다")
    void refresh_notApproved() {
        User pending = User.builder()
                .username(USERNAME).password("encoded").name("테스트")
                .slackId("U000TEST123").affiliationName("테스트업체")
                .status(Status.PENDING)
                .build();

        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.of(OLD_REFRESH_TOKEN));
        given(userRepository.findByUsernameAndDeletedAtIsNull(USERNAME)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_APPROVED);
    }
}