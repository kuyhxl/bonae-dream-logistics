package com.bonae.logistics.hub.domain.vo;

import com.bonae.logistics.hub.domain.entity.HubRoute;

import java.util.UUID;

/**
 * 경로 탐색에 사용하는 허브 간 방향 간선의 불변 값 객체.
 */
public record HubRouteEdge(
        UUID fromHubId,
        UUID toHubId,
        Integer distanceMeters,
        Integer durationSeconds
) {

    public static HubRouteEdge from(HubRoute route) {
        return new HubRouteEdge(
                route.getDepartureHub().getId(),
                route.getArrivalHub().getId(),
                route.getDistanceMeters(),
                route.getDurationSeconds()
        );
    }
}