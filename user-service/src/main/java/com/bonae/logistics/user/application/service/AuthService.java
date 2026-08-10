package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import com.bonae.logistics.user.infrastructure.persistence.RefreshTokenStore;
import com.bonae.logistics.user.infrastructure.persistence.TokenBlacklistStore;
import com.bonae.logistics.user.infrastructure.security.JwtProvider;
import com.bonae.logistics.user.presentation.dto.request.LoginRequest;
import com.bonae.logistics.user.presentation.dto.request.LogoutRequest;
import com.bonae.logistics.user.presentation.dto.request.RefreshRequest;
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.LoginResponse;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
import com.bonae.logistics.user.presentation.dto.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistStore tokenBlacklistStore;

    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        String username = signupRequest.getUsername();

        // username 중복 확인
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
        }

        String password = passwordEncoder.encode(signupRequest.getPassword());

        User user = User.builder()
                .username(username)
                .password(password)
                .name(signupRequest.getName().trim())
                .slackId(signupRequest.getSlackId().trim())
                .affiliationName(signupRequest.getAffiliationName().trim())
                .build();

        try {
            return new SignupResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED);
        }
    }

    public LoginResponse login(@Valid LoginRequest loginRequest) {

        // 검토
        // 존재하는 username인지
        User user = userRepository.findByUsernameAndDeletedAtIsNull(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 비밀번호가 일치하는지
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 승인됬는지
        if (user.getStatus() != Status.APPROVED) {
            throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
        }

        // accessToken, refreshToken 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole(), user.getHubId(), user.getCompanyId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        // redis에 저장 (refreshToken, 만료 시간)
        refreshTokenStore.save(user.getUsername(), refreshToken, jwtProperties.getRefreshTokenExpiration());

        // 응답 DTO
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType(JwtProvider.TOKEN_TYPE)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .refreshToken(refreshToken)
                .refreshExpiresIn(jwtProperties.getRefreshTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public void logout(String authorizationHeader, LogoutRequest logoutRequest) {
        String refreshToken = logoutRequest.getRefreshToken();

        // 서명·만료·타입 검증 후 username 추출
        String username = jwtProvider.parseRefreshToken(refreshToken).getSubject();

        // refreshToken 저장소에 있는 토큰과 동일한지 확인
        String storedToken = refreshTokenStore.find(username).orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // refreshToken 삭제
        refreshTokenStore.delete(username);

        // accessToken 즉시 무효화 -> redis blacklist에 담기!
        blacklistAccessToken(authorizationHeader);
    }

    private void blacklistAccessToken(String authorizationHeader) {
        String bearerPrefix = JwtProvider.TOKEN_TYPE + " ";

        if (authorizationHeader == null || !authorizationHeader.startsWith(bearerPrefix)) {
            return;
        }

        String accessToken = authorizationHeader.substring(bearerPrefix.length()).trim();

        jwtProvider.parseAccessToken(accessToken)
                .ifPresent(claims -> tokenBlacklistStore.add(claims.getId(), jwtProvider.remainingMillis(claims)));
    }

    public TokenResponse refresh(String authorizationHeader, RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        // 1. 서명·만료·타입(typ=REFRESH) 검증 후 username 추출
        String username = jwtProvider.parseRefreshToken(refreshToken).getSubject();

        // 2. 저장소에 있는 토큰과 동일한지 확인 (로그아웃된 토큰 차단)
        String storedToken = refreshTokenStore.find(username).orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 3. 이미 회전된 옛 토큰이 다시 들어옴 = 탈취 의심 -> 저장된 토큰까지 폐기하고 재로그인 유도
        if (!storedToken.equals(refreshToken)) {
            refreshTokenStore.delete(username);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 4. 토큰 발급 이후 탈퇴/권한이 변경됐을 수 있으므로 DB 상태를 다시 확인
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != Status.APPROVED) {
            throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
        }

        // 5. 최신 role, hubId, companyId로 accessToken 재발급
        String newAccessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole(), user.getHubId(), user.getCompanyId());

        // 6. refreshToken도 함께 교체 (RTR: Refresh Token Rotation)
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUsername());
        refreshTokenStore.save(user.getUsername(), newRefreshToken, jwtProperties.getRefreshTokenExpiration()); // refreshTokenStore.save()로 덮어쓰기 -> 기존 refreshToken은 자동으로 무효화됩니다.

        // 7. 회전 전 accessToken 즉시 무효화 -> 한 사용자에게 유효한 accessToken이 둘 이상 존재하지 않게 한다
        blacklistAccessToken(authorizationHeader);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType(JwtProvider.TOKEN_TYPE)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .refreshToken(newRefreshToken)
                .refreshExpiresIn(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();
    }
}
