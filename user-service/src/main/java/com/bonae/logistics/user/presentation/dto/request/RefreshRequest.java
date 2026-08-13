package com.bonae.logistics.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "토큰 재발급 요청")
public class RefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    @Schema(description = "재발급에 사용할 리프레시 토큰. 재발급 성공 시 새 토큰으로 교체된다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
