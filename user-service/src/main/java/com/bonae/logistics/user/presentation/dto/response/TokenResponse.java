package com.bonae.logistics.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "토큰 재발급 응답. 액세스 토큰과 리프레시 토큰이 함께 교체된다.")
public class TokenResponse {

    @Schema(description = "새로 발급된 액세스 토큰")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "3600")
    private long expiresIn; // 초

    @Schema(description = "새로 발급된 리프레시 토큰. 재발급 시점에 이전 토큰은 무효화된다.")
    private String refreshToken;

    @Schema(description = "리프레시 토큰 만료까지 남은 시간(초)", example = "604800")
    private long refreshExpiresIn;  // 초
}
