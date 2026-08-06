package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .name(signupRequest.getName())
                .slackId(signupRequest.getSlackId())
                .affiliationName(signupRequest.getAffiliationName())
                .build();

        return new SignupResponse(userRepository.save(user));
    }
}
