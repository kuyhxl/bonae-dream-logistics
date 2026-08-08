package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.entity.HubRoute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "이동정보 상세 응답")
public class HubRouteDetailResponse {

    @Schema(description = "이동정보 ID")
    private UUID hubRouteId;

    @Schema(description = "출발 허브 ID")
    private UUID departureHubId;

    @Schema(description = "도착 허브 ID")
    private UUID arrivalHubId;

    @Schema(description = "이동 거리(미터)")
    private Integer distanceMeters;

    @Schema(description = "예상 소요 시간(초)")
    private Integer durationSeconds;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "생성자")
    private String createdBy;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    @Schema(description = "수정자")
    private String updatedBy;

    public static HubRouteDetailResponse from(HubRoute hubRoute) {
        return HubRouteDetailResponse.builder()
                .hubRouteId(hubRoute.getId())
                .departureHubId(hubRoute.getDepartureHub().getId())
                .arrivalHubId(hubRoute.getArrivalHub().getId())
                .distanceMeters(hubRoute.getDistanceMeters())
                .durationSeconds(hubRoute.getDurationSeconds())
                .createdAt(hubRoute.getCreatedAt())
                .createdBy(hubRoute.getCreatedBy())
                .updatedAt(hubRoute.getUpdatedAt())
                .updatedBy(hubRoute.getUpdatedBy())
                .build();
    }
}