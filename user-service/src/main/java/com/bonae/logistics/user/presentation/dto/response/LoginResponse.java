package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "액세스 토큰. 이후 요청의 Authorization 헤더에 `Bearer {토큰}` 형태로 담는다.")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "3600")
    private long expiresIn; // 초

    @Schema(description = "리프레시 토큰. 액세스 토큰 재발급에 사용하며 서버에도 저장된다.")
    private String refreshToken;

    @Schema(description = "리프레시 토큰 만료까지 남은 시간(초)", example = "604800")
    private long refreshExpiresIn; // 초

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID userId;

    @Schema(description = "아이디", example = "hyerim01")
    private String username;

    @Schema(description = "역할", example = "HUB_MANAGER")
    private Role role;
}
