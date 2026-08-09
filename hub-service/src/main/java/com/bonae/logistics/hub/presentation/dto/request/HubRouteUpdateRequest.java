package com.bonae.logistics.hub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이동정보 수정 요청")
public class HubRouteUpdateRequest {

    @Positive(message = "이동 거리는 0보다 커야 합니다.")
    @Schema(description = "이동 거리(미터)", example = "158000")
    private Integer distanceMeters;

    @Positive(message = "예상 소요 시간은 0보다 커야 합니다.")
    @Schema(description = "예상 소요 시간(초)", example = "6300")
    private Integer durationSeconds;

    public boolean isEmpty() {
        return distanceMeters == null && durationSeconds == null;
    }
}