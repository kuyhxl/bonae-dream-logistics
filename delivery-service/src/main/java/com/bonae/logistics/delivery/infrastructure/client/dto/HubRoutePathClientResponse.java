package com.bonae.logistics.delivery.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubRoutePathClientResponse {

    private Long totalDistanceMeters;
    private Long totalDurationSeconds;
    private Double totalDistanceKm;
    private Long totalDurationMin;
    private List<HubRoutePathSegmentClientResponse> segments;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HubRoutePathSegmentClientResponse {
        private Integer sequence;
        private UUID fromHubId;
        private UUID toHubId;
        private Integer distanceMeters;
        private Integer durationSeconds;
        private Double distanceKm;
        private Integer durationMin;
    }
}
