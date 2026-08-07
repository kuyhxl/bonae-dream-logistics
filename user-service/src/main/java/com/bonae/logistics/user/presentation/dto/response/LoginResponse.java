package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn; // 초
    private String refreshToken;
    private long refreshExpiresIn; // 초
    private UUID userId;
    private String username;
    private Role role;
}
