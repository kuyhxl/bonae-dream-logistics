package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.entity.HubRoute;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Getter
@Builder
public class HubRoutePathResponse {

    private Long totalDistanceMeters;
    private Long totalDurationSeconds;
    private Double totalDistanceKm;
    private Long totalDurationMin;
    private List<HubRoutePathSegment> segments;

    public static HubRoutePathResponse from(List<HubRoute> path) {
        long totalDistanceMeters = 0L;
        long totalDurationSeconds = 0L;

        for (HubRoute route : path) {
            totalDistanceMeters += route.getDistanceMeters();
            totalDurationSeconds += route.getDurationSeconds();
        }

        double totalDistanceKm = Math.round(totalDistanceMeters / 1000.0 * 10) / 10.0;
        long totalDurationMin = totalDurationSeconds / 60 + (totalDurationSeconds % 60 == 0 ? 0 : 1);

        List<HubRoutePathSegment> segments = IntStream.range(0, path.size())
                .mapToObj(i -> HubRoutePathSegment.from(i + 1, path.get(i)))
                .toList();

        return HubRoutePathResponse.builder()
                .totalDistanceMeters(totalDistanceMeters)
                .totalDurationSeconds(totalDurationSeconds)
                .totalDistanceKm(totalDistanceKm)
                .totalDurationMin(totalDurationMin)
                .segments(segments)
                .build();
    }

    @Getter
    @Builder
    public static class HubRoutePathSegment {
        private Integer sequence;
        private UUID fromHubId;         // departureHubId
        private UUID toHubId;           // arrivalHubId
        private Integer distanceMeters;
        private Integer durationSeconds;
        private Double distanceKm;
        private Integer durationMin;

        public static HubRoutePathSegment from(int sequence, HubRoute hubRoute) {
            return HubRoutePathSegment.builder()
                    .sequence(sequence)
                    .fromHubId(hubRoute.getDepartureHub().getId())
                    .toHubId(hubRoute.getArrivalHub().getId())
                    .distanceMeters(hubRoute.getDistanceMeters())
                    .durationSeconds(hubRoute.getDurationSeconds())
                    .distanceKm(Math.round(hubRoute.getDistanceMeters() / 1000.0 * 10) / 10.0)
                    .durationMin((int) Math.ceil(hubRoute.getDurationSeconds() / 60.0))
                    .build();
        }
    }
}
