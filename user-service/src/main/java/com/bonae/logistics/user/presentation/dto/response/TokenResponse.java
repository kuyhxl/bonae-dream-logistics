package com.bonae.logistics.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn; // 초
    private String refreshToken;
    private long refreshExpiresIn;  // 초
}
