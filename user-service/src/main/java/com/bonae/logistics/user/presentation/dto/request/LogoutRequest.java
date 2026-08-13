package com.bonae.logistics.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그아웃 요청")
public class LogoutRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    @Schema(description = "폐기할 리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
