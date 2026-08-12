package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
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

    public static HubRoutePathResponse from(List<HubRouteEdge> path) {
        long totalDistanceMeters = 0L;
        long totalDurationSeconds = 0L;

        for (HubRouteEdge edge : path) {
            totalDistanceMeters += edge.distanceMeters();
            totalDurationSeconds += edge.durationSeconds();
        }

        double totalDistanceKm = Math.round(totalDistanceMeters / 1000.0 * 10) / 10.0;
        long totalDurationMin = totalDurationSeconds / 60 + (totalDurationSeconds % 60 == 0 ? 0 : 1);

        List<HubRoutePathSegment> segments = IntStream.range(0, path.size())
                .mapToObj(index -> HubRoutePathSegment.from(index + 1, path.get(index)))
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

        public static HubRoutePathSegment from(int sequence, HubRouteEdge edge) {
            return HubRoutePathSegment.builder()
                    .sequence(sequence)
                    .fromHubId(edge.fromHubId())
                    .toHubId(edge.toHubId())
                    .distanceMeters(edge.distanceMeters())
                    .durationSeconds(edge.durationSeconds())
                    .distanceKm(Math.round(edge.distanceMeters() / 1000.0 * 10) / 10.0)
                    .durationMin((int) Math.ceil(edge.durationSeconds() / 60.0))
                    .build();
        }
    }
}