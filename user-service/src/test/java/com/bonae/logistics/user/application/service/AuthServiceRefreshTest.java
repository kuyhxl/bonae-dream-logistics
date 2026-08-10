package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import com.bonae.logistics.user.infrastructure.persistence.RefreshTokenStore;
import com.bonae.logistics.user.infrastructure.persistence.TokenBlacklistStore;
import com.bonae.logistics.user.infrastructure.security.JwtProvider;
import com.bonae.logistics.user.presentation.dto.request.RefreshRequest;
import com.bonae.logistics.user.presentation.dto.response.TokenResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private static final String OLD_ACCESS_TOKEN = "old.access.token";
    private static final String AUTH_HEADER = JwtProvider.TOKEN_TYPE + " " + OLD_ACCESS_TOKEN;
    private static final String OLD_ACCESS_JTI = "old-access-jti";
    private static final long ACCESS_EXP = 3600000L;
    private static final long REFRESH_EXP = 604800000L;
    private static final long REMAINING_MILLIS = 1200000L;
    private static final UUID HUB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COMPANY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
    private TokenBlacklistStore tokenBlacklistStore;
    @Mock
    private Claims claims;
    @Mock
    private Claims accessClaims;

    /* refreshToken의 서명·만료·타입 검증을 통과한 상태를 만든다 */
    private void givenValidRefreshToken() {
        given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN)).willReturn(claims);
        given(claims.getSubject()).willReturn(USERNAME);
    }

    /* 저장소에 보관 중인 refreshToken을 지정한다 */
    private void givenStoredToken(String storedToken) {
        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.of(storedToken));
    }

    /* 재발급에 필요한 나머지 스텁을 채운다 */
    private void givenIssuableUser(Role role, UUID hubId, UUID companyId) {
        given(userRepository.findByUsernameAndDeletedAtIsNull(USERNAME))
                .willReturn(Optional.of(user(role, Status.APPROVED, hubId, companyId)));
        given(jwtProvider.createAccessToken(USERNAME, role, hubId, companyId)).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.createRefreshToken(USERNAME)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProperties.getAccessTokenExpiration()).willReturn(ACCESS_EXP);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(REFRESH_EXP);
    }

    private User user(Role role, Status status, UUID hubId, UUID companyId) {
        return User.builder()
                .username(USERNAME)
                .password("encoded")
                .name("테스트")
                .slackId("U000TEST123")
                .affiliationName("테스트업체")
                .role(role)
                .status(status)
                .hubId(hubId)
                .companyId(companyId)
                .build();
    }

    /* 실패 케이스에서는 토큰이 새로 발급되지도, 기존 토큰이 폐기되지도 않아야 한다 */
    private void thenNothingIssued() {
        then(refreshTokenStore).should(never()).save(anyString(), anyString(), anyLong());
        then(tokenBlacklistStore).shouldHaveNoInteractions();
    }

    static Stream<Arguments> affiliations() {
        return Stream.of(
                Arguments.of(Role.HUB_MANAGER, HUB_ID, null),
                Arguments.of(Role.DELIVERY_MANAGER, HUB_ID, null),
                Arguments.of(Role.COMPANY_MANAGER, null, COMPANY_ID),
                Arguments.of(Role.MASTER, null, null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("affiliations")
    @DisplayName("유효한 refreshToken이면 DB의 최신 role·hubId·companyId로 토큰을 모두 새로 발급한다")
    void refresh_success(Role role, UUID hubId, UUID companyId) {
        // given
        givenValidRefreshToken();
        givenStoredToken(OLD_REFRESH_TOKEN);
        givenIssuableUser(role, hubId, companyId);

        // when - Authorization 헤더 없이 호출 (permit-all 경로라 헤더가 없을 수 있다)
        TokenResponse response = authService.refresh(null, new RefreshRequest(OLD_REFRESH_TOKEN));

        // then
        assertThat(response.getAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(response.getTokenType()).isEqualTo(JwtProvider.TOKEN_TYPE);
        assertThat(response.getExpiresIn()).isEqualTo(ACCESS_EXP / 1000);
        assertThat(response.getRefreshExpiresIn()).isEqualTo(REFRESH_EXP / 1000);

        // 소속 클레임은 refreshToken이 아니라 DB 조회 결과가 그대로 전달되어야 한다
        then(jwtProvider).should().createAccessToken(USERNAME, role, hubId, companyId);

        // refreshToken은 새 값으로 덮어써서 회전시킨다 (RTR)
        then(refreshTokenStore).should().save(USERNAME, NEW_REFRESH_TOKEN, REFRESH_EXP);
        then(refreshTokenStore).should(never()).delete(anyString());

        // 헤더가 없으면 무효화할 accessToken도 없다
        then(tokenBlacklistStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("재발급에 성공하면 회전 전 accessToken을 남은 수명만큼 블랙리스트에 등록한다")
    void refresh_blacklistsPreviousAccessToken() {
        // given
        givenValidRefreshToken();
        givenStoredToken(OLD_REFRESH_TOKEN);
        givenIssuableUser(Role.HUB_MANAGER, HUB_ID, null);
        given(jwtProvider.parseAccessToken(OLD_ACCESS_TOKEN)).willReturn(Optional.of(accessClaims));
        given(accessClaims.getId()).willReturn(OLD_ACCESS_JTI);
        given(jwtProvider.remainingMillis(accessClaims)).willReturn(REMAINING_MILLIS);

        // when
        TokenResponse response = authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN));

        // then - 한 사용자에게 유효한 accessToken이 둘 이상 남지 않아야 한다
        assertThat(response.getAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        then(tokenBlacklistStore).should().add(OLD_ACCESS_JTI, REMAINING_MILLIS);
    }

    @Test
    @DisplayName("헤더의 accessToken이 이미 만료·위조된 경우 블랙리스트 등록 없이 재발급된다")
    void refresh_withUnparsableAccessTokenHeader() {
        // given
        givenValidRefreshToken();
        givenStoredToken(OLD_REFRESH_TOKEN);
        givenIssuableUser(Role.COMPANY_MANAGER, null, COMPANY_ID);
        given(jwtProvider.parseAccessToken(OLD_ACCESS_TOKEN)).willReturn(Optional.empty());

        // when
        TokenResponse response = authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN));

        // then - 무효화할 대상이 없을 뿐 재발급 자체는 정상 동작해야 한다
        assertThat(response.getAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        then(tokenBlacklistStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("만료된 refreshToken이면 저장소를 건드리지 않고 예외가 전파된다")
    void refresh_expiredToken() {
        // given - 검증 단계에서 바로 막히므로 givenValidRefreshToken()을 쓰지 않는다
        given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN))
                .willThrow(new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_REFRESH_TOKEN);

        then(refreshTokenStore).shouldHaveNoInteractions();
        then(tokenBlacklistStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("위조된 refreshToken이면 저장소를 건드리지 않고 예외가 전파된다")
    void refresh_invalidToken() {
        // given
        given(jwtProvider.parseRefreshToken(OLD_REFRESH_TOKEN))
                .willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

        then(refreshTokenStore).shouldHaveNoInteractions();
        then(tokenBlacklistStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("저장소에 refreshToken이 없으면(로그아웃됨) REFRESH_TOKEN_NOT_FOUND")
    void refresh_notFoundInStore() {
        // given
        givenValidRefreshToken();
        given(refreshTokenStore.find(USERNAME)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        thenNothingIssued();
    }

    @Test
    @DisplayName("이미 회전된 옛 refreshToken이 재사용되면 저장된 토큰까지 폐기한다")
    void refresh_reuseDetected() {
        // given - 저장소에는 이미 새 토큰이 들어 있는데 옛 토큰으로 재발급을 시도한다
        givenValidRefreshToken();
        givenStoredToken(NEW_REFRESH_TOKEN);

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        // 탈취 의심 상황이므로 저장된 토큰까지 지워 재로그인을 강제한다
        then(refreshTokenStore).should().delete(USERNAME);
        thenNothingIssued();
    }

    @Test
    @DisplayName("탈퇴한 사용자의 refreshToken이면 USER_NOT_FOUND")
    void refresh_userNotFound() {
        // given - 저장소에는 토큰이 남아 있지만 DB에서는 논리 삭제된 상태
        givenValidRefreshToken();
        givenStoredToken(OLD_REFRESH_TOKEN);
        given(userRepository.findByUsernameAndDeletedAtIsNull(USERNAME)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        thenNothingIssued();
    }

    @Test
    @DisplayName("승인 상태가 아닌 사용자는 재발급할 수 없다")
    void refresh_notApproved() {
        // given
        givenValidRefreshToken();
        givenStoredToken(OLD_REFRESH_TOKEN);
        given(userRepository.findByUsernameAndDeletedAtIsNull(USERNAME))
                .willReturn(Optional.of(user(null, Status.PENDING, null, null)));

        // when & then
        assertThatThrownBy(() -> authService.refresh(AUTH_HEADER, new RefreshRequest(OLD_REFRESH_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_APPROVED);

        thenNothingIssued();
    }
}
