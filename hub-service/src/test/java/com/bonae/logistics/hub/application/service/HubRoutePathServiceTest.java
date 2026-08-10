package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HubRoutePathServiceTest {

    @Mock
    private HubRepository hubRepository;

    @Mock
    private HubRoutePathFinder hubRoutePathFinder;

    @InjectMocks
    private HubRoutePathService hubRoutePathService;

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
    @DisplayName("출발/도착 허브가 모두 존재하면 경로 응답을 반환한다")
    void returnsPathResponseWhenBothHubsExist() {
        Hub hubA = createHub(37.0, 127.0);
        Hub hubB = createHub(37.1, 127.1);
        HubRoute route = createRoute(hubA, hubB, 100);

        when(hubRepository.existsByIdAndDeletedAtIsNull(any(UUID.class)))
                .thenReturn(true);
        when(hubRoutePathFinder.findShortestPath(hubA.getId(), hubB.getId()))
                .thenReturn(List.of(route));

        HubRoutePathResponse result = hubRoutePathService.findShortestPath(hubA.getId(), hubB.getId());

        assertThat(result.getTotalDistanceMeters()).isEqualTo(route.getDistanceMeters());
        assertThat(result.getTotalDurationSeconds()).isEqualTo(100);
        assertThat(result.getSegments()).hasSize(1);
    }

    @Test
    @DisplayName("출발 허브가 존재하지 않으면 HUB_NOT_FOUND 예외가 발생한다")
    void throwsNotFoundWhenDepartureHubDoesNotExist() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(departureHubId))
                .thenReturn(false);

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(departureHubId, arrivalHubId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.HUB_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("도착 허브가 존재하지 않으면 HUB_NOT_FOUND 예외가 발생하고, 존재 확인은 두 번 호출된다")
    void throwsNotFoundWhenArrivalHubDoesNotExist() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(departureHubId)).thenReturn(true);
        when(hubRepository.existsByIdAndDeletedAtIsNull(arrivalHubId)).thenReturn(false);

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(departureHubId, arrivalHubId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.HUB_NOT_FOUND)
                );

        verify(hubRepository, times(2)).existsByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("동일한 허브 ID면 도착 허브 존재 확인은 생략되고, 빈 경로 기반 응답을 반환한다")
    void skipsArrivalCheckWhenSameHubId() {
        UUID hubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(hubId)).thenReturn(true);
        when(hubRoutePathFinder.findShortestPath(hubId, hubId)).thenReturn(List.of());

        HubRoutePathResponse result = hubRoutePathService.findShortestPath(hubId, hubId);

        assertThat(result.getTotalDistanceKm()).isEqualTo(0.0);
        assertThat(result.getTotalDurationMin()).isEqualTo(0);
        assertThat(result.getSegments()).isEmpty();

        verify(hubRepository, times(1)).existsByIdAndDeletedAtIsNull(any());
    }
}