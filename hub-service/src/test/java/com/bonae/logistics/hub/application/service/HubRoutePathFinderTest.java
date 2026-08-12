package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubRoutePathFinderTest {

    private final HubRoutePathFinder hubRoutePathFinder = new HubRoutePathFinder();

    private HubRouteEdge createEdge(UUID departureHubId, UUID arrivalHubId, int durationSeconds) {
        return new HubRouteEdge(departureHubId, arrivalHubId, 1_000, durationSeconds);
    }

    @Test
    @DisplayName("출발허브와 도착허브 ID가 같으면 빈 경로를 반환한다")
    void returnsEmptyPathWhenSameHub() {
        UUID hubId = UUID.randomUUID();

        List<HubRouteEdge> result = hubRoutePathFinder.findShortestPath(List.of(), hubId, hubId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("직접 연결된 두 허브는 그 경로 하나만 결과로 나온다")
    void findsDirectPath() {
        UUID hubAId = UUID.randomUUID();
        UUID hubBId = UUID.randomUUID();
        HubRouteEdge routeAB = createEdge(hubAId, hubBId, 5);

        List<HubRouteEdge> result = hubRoutePathFinder.findShortestPath(List.of(routeAB), hubAId, hubBId);

        assertThat(result).containsExactly(routeAB);
    }

    @Test
    @DisplayName("직접 가는 것보다 거쳐 가는 게 더 빠르면, 거쳐 가는 경로를 순서대로 반환한다")
    void prefersShorterWaypointPath() {
        UUID hubAId = UUID.randomUUID();
        UUID hubBId = UUID.randomUUID();
        UUID hubCId = UUID.randomUUID();

        HubRouteEdge routeAC = createEdge(hubAId, hubCId, 10);
        HubRouteEdge routeAB = createEdge(hubAId, hubBId, 3);
        HubRouteEdge routeBC = createEdge(hubBId, hubCId, 4);

        List<HubRouteEdge> result = hubRoutePathFinder.findShortestPath(List.of(routeAC, routeAB, routeBC), hubAId, hubCId);

        assertThat(result).containsExactly(routeAB, routeBC);
    }

    @Test
    @DisplayName("갈 수 있는 길이 아예 없으면 HUB_ROUTE_NOT_FOUND 예외가 발생한다")
    void throwsNotFoundWhenUnreachable() {
        UUID hubAId = UUID.randomUUID();
        UUID hubCId = UUID.randomUUID();

        assertThatThrownBy(() -> hubRoutePathFinder.findShortestPath(List.of(), hubAId, hubCId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.HUB_ROUTE_NOT_FOUND));
    }

    @Test
    @DisplayName("경로 역추적 중 이전 간선이 없으면 내부 상태 예외가 발생한다")
    void throwsExceptionWhenPreviousEdgeIsMissing() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        Map<UUID, HubRouteEdge> previous = Map.of();

        assertThatThrownBy(() -> hubRoutePathFinder.reconstructPath(departureHubId, arrivalHubId, previous))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(arrivalHubId.toString());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    @DisplayName("경로 역추적 중 이전 간선이 순환하면 내부 상태 예외가 발생한다")
    void throwsExceptionWhenPreviousEdgesFormCycle() {
        UUID departureHubId = UUID.randomUUID();
        UUID hubAId = UUID.randomUUID();
        UUID hubBId = UUID.randomUUID();

        HubRouteEdge routeBToA = createEdge(hubBId, hubAId, 5);
        HubRouteEdge routeAToB = createEdge(hubAId, hubBId, 5);

        // 역추적이 hubA → hubB → hubA로 반복되어 출발 허브에 도달하지 않는 순환을 구성한다.
        Map<UUID, HubRouteEdge> previous = Map.of(
                hubAId, routeBToA,
                hubBId, routeAToB
        );

        assertThatThrownBy(() -> hubRoutePathFinder.reconstructPath(departureHubId, hubAId, previous))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("순환")
                .hasMessageContaining(hubAId.toString());
    }

    @Test
    @DisplayName("누적 이동시간이 int 범위를 넘어도 실제 소요시간이 짧은 경로를 선택한다")
    void choosesCorrectPathWithLargeDurations() {
        UUID hubAId = UUID.randomUUID();
        UUID hubBId = UUID.randomUUID();
        UUID hubCId = UUID.randomUUID();

        HubRouteEdge routeAC = createEdge(hubAId, hubCId, 2_000_000_000);
        HubRouteEdge routeAB = createEdge(hubAId, hubBId, 1_500_000_000);
        HubRouteEdge routeBC = createEdge(hubBId, hubCId, 1_500_000_000);

        // A→B→C는 총 30억 초로 int 범위를 넘으므로, 총 20억 초인 A→C가 최단경로다.
        List<HubRouteEdge> result = hubRoutePathFinder.findShortestPath(List.of(routeAC, routeAB, routeBC), hubAId, hubCId);

        assertThat(result).containsExactly(routeAC);
    }
}