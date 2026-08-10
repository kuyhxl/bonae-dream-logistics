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
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.LoginResponse;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
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

    @Transactional(readOnly = true)
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
}
