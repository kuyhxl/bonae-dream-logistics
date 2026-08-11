package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HubRoutePathFinderTest {
    @Mock
    private HubRouteRepository hubRouteRepository;

    @InjectMocks
    private HubRoutePathFinder hubRoutePathFinder;

    private Hub createHub(double latitude, double longitude) {
        Hub hub = Hub.create("테스트허브", "테스트주소", latitude, longitude);
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        return hub;
    }

    private HubRoute createRoute(Hub departure, Hub arrival, int durationSeconds) {
        HubRoute route = HubRoute.create(departure, arrival);
        route.update(route.getDistanceMeters(), durationSeconds);
        return route;
    }

    @Test
    @DisplayName("출발허브와 도착허브 ID가 같으면 빈 경로를 반환한다")
    void returnsEmptyPathWhenSameHub() {
        UUID hubId = UUID.randomUUID();

        List<HubRoute> result = hubRoutePathFinder.findShortestPath(hubId, hubId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("직접 연결된 두 허브는 그 경로 하나만 결과로 나온다")
    void findsDirectPath() {
        Hub hubA = createHub(1.0,1.0);
        Hub hubB = createHub(2.0,2.0);
        HubRoute routeAB = createRoute(hubA, hubB, 5);

        when(hubRouteRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(routeAB));

        List<HubRoute> result = hubRoutePathFinder.findShortestPath(hubA.getId(), hubB.getId());

        assertThat(result).containsExactly(routeAB);
    }

    @Test
    @DisplayName("직접 가는 것보다 거쳐 가는 게 더 빠르면, 거쳐 가는 경로를 순서대로 반환한다")
    void prefersShorterWaypointPath() {
        Hub hubA = createHub(1.0,1.0);
        Hub hubB = createHub(2.0,2.0);
        Hub hubC = createHub(3.0,3.0);

        HubRoute routeAC = createRoute(hubA, hubC, 10);
        HubRoute routeAB = createRoute(hubA, hubB, 3);
        HubRoute routeBC = createRoute(hubB, hubC, 4);

        when(hubRouteRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(routeAC, routeAB, routeBC));

        List<HubRoute> result = hubRoutePathFinder.findShortestPath(hubA.getId(), hubC.getId());

        assertThat(result).containsExactly(routeAB, routeBC);
    }

    @Test
    @DisplayName("갈 수 있는 길이 아예 없으면 HUB_ROUTE_NOT_FOUND 예외가 발생한다")
    void throwsNotFoundWhenUnreachable() {
        Hub hubA = createHub(1.0,1.0);
        Hub hubC = createHub(2.0,2.0);

        when(hubRouteRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of());

        assertThatThrownBy(() -> hubRoutePathFinder.findShortestPath(hubA.getId(), hubC.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.HUB_ROUTE_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("경로 역추적 중 이전 간선이 없으면 내부 상태 예외가 발생한다")
    void throwsExceptionWhenPreviousEdgeIsMissing() {
        Hub departureHub = createHub(1.0, 1.0);
        Hub arrivalHub = createHub(2.0, 2.0);

        Map<UUID, HubRoute> previous = Map.of();

        assertThatThrownBy(() ->
                hubRoutePathFinder.reconstructPath(
                        departureHub.getId(),
                        arrivalHub.getId(),
                        previous
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(arrivalHub.getId().toString());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    @DisplayName("경로 역추적 중 이전 간선이 순환하면 내부 상태 예외가 발생한다")
    void throwsExceptionWhenPreviousEdgesFormCycle() {
        Hub departureHub = createHub(1.0, 1.0);
        Hub hubA = createHub(2.0, 2.0);
        Hub hubB = createHub(3.0, 3.0);

        HubRoute routeBToA = createRoute(hubB, hubA, 5);
        HubRoute routeAToB = createRoute(hubA, hubB, 5);

        // 역추적이 hubA → hubB → hubA로 반복되어 출발 허브에 도달하지 않는 순환을 구성한다.
        Map<UUID, HubRoute> previous = Map.of(
                hubA.getId(), routeBToA,
                hubB.getId(), routeAToB
        );

        assertThatThrownBy(() ->
                hubRoutePathFinder.reconstructPath(
                        departureHub.getId(),
                        hubA.getId(),
                        previous
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("순환")
                .hasMessageContaining(hubA.getId().toString());
    }

    @Test
    @DisplayName("누적 이동시간이 int 범위를 넘어도 실제 소요시간이 짧은 경로를 선택한다")
    void choosesCorrectPathWithLargeDurations() {
        Hub hubA = createHub(1.0, 1.0);
        Hub hubB = createHub(2.0, 2.0);
        Hub hubC = createHub(3.0, 3.0);

        HubRoute routeAC = createRoute(hubA, hubC, 2_000_000_000);
        HubRoute routeAB = createRoute(hubA, hubB, 1_500_000_000);
        HubRoute routeBC = createRoute(hubB, hubC, 1_500_000_000);

        // A→B→C는 총 30억 초로 int 범위를 넘으므로, 총 20억 초인 A→C가 최단경로다.
        when(hubRouteRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(routeAC, routeAB, routeBC));

        List<HubRoute> result =
                hubRoutePathFinder.findShortestPath(hubA.getId(), hubC.getId());

        assertThat(result).containsExactly(routeAC);
    }
}
