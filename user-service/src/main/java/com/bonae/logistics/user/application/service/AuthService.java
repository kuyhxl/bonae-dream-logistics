package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.config.JwtProperties;
import com.bonae.logistics.user.infrastructure.persistence.RefreshTokenStore;
import com.bonae.logistics.user.infrastructure.security.JwtProvider;
import com.bonae.logistics.user.presentation.dto.request.LoginRequest;
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

    @Transactional
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
        String accessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole());
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
}
