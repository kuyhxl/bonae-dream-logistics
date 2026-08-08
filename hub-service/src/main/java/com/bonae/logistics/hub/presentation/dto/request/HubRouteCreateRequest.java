package com.bonae.logistics.hub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Schema(description = "이동정보 생성 요청")
public class HubRouteCreateRequest {
    @NotNull(message = "출발 허브 ID는 필수입니다.")
    @Schema(description = "출발 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID departureHubId;

    @NotNull(message = "도착 허브 ID는 필수입니다.")
    @Schema(description = "도착 허브 ID", example = "39f27e08-8164-4e94-bd34-a0c42d3a176b")
    private UUID arrivalHubId;
}
