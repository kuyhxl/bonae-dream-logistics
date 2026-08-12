package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private HubRoutePathProvider hubRoutePathProvider;

    @InjectMocks
    private HubRoutePathService hubRoutePathService;

    @Test
    @DisplayName("출발/도착 허브가 존재하면 검증 후 최적 경로 결과를 반환한다")
    void returnsPathResponseWhenBothHubsExist() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        HubRoutePathResponse expected = pathResponse(departureHubId, arrivalHubId);

        when(hubRepository.existsByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(true);
        when(hubRoutePathProvider.getShortestPath(departureHubId, arrivalHubId)).thenReturn(expected);

        HubRoutePathResponse result = hubRoutePathService.findShortestPath(departureHubId, arrivalHubId);

        assertThat(result).isSameAs(expected);
        verify(hubRepository, times(2)).existsByIdAndDeletedAtIsNull(any());
        verify(hubRoutePathProvider).getShortestPath(departureHubId, arrivalHubId);
    }

    @Test
    @DisplayName("출발 허브가 존재하지 않으면 경로 결과 캐시를 조회하지 않는다")
    void throwsNotFoundWhenDepartureHubDoesNotExist() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(departureHubId)).thenReturn(false);

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(departureHubId, arrivalHubId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.HUB_NOT_FOUND));

        verifyNoInteractions(hubRoutePathProvider);
    }

    @Test
    @DisplayName("도착 허브가 존재하지 않으면 경로 결과 캐시를 조회하지 않는다")
    void throwsNotFoundWhenArrivalHubDoesNotExist() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(departureHubId)).thenReturn(true);
        when(hubRepository.existsByIdAndDeletedAtIsNull(arrivalHubId)).thenReturn(false);

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(departureHubId, arrivalHubId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.HUB_NOT_FOUND));

        verify(hubRepository, times(2)).existsByIdAndDeletedAtIsNull(any());
        verifyNoInteractions(hubRoutePathProvider);
    }

    @Test
    @DisplayName("동일한 허브 ID면 존재만 검증하고 빈 경로 응답을 반환한다")
    void skipsPathCacheWhenSameHubId() {
        UUID hubId = UUID.randomUUID();

        when(hubRepository.existsByIdAndDeletedAtIsNull(hubId)).thenReturn(true);

        HubRoutePathResponse result = hubRoutePathService.findShortestPath(hubId, hubId);

        assertThat(result.getTotalDistanceMeters()).isZero();
        assertThat(result.getTotalDurationSeconds()).isZero();
        assertThat(result.getTotalDistanceKm()).isEqualTo(0.0);
        assertThat(result.getTotalDurationMin()).isZero();
        assertThat(result.getSegments()).isEmpty();

        verify(hubRepository).existsByIdAndDeletedAtIsNull(hubId);
        verifyNoInteractions(hubRoutePathProvider);
    }

    @Test
    @DisplayName("경로 결과가 캐시에 있더라도 출발/도착 허브 존재를 먼저 검증한다")
    void validatesHubsBeforeUsingPathCache() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        HubRoutePathResponse cached = pathResponse(departureHubId, arrivalHubId);

        when(hubRepository.existsByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(true);
        when(hubRoutePathProvider.getShortestPath(departureHubId, arrivalHubId)).thenReturn(cached);

        HubRoutePathResponse result = hubRoutePathService.findShortestPath(departureHubId, arrivalHubId);

        assertThat(result).isSameAs(cached);

        var inOrder = inOrder(hubRepository, hubRoutePathProvider);
        inOrder.verify(hubRepository).existsByIdAndDeletedAtIsNull(departureHubId);
        inOrder.verify(hubRepository).existsByIdAndDeletedAtIsNull(arrivalHubId);
        inOrder.verify(hubRoutePathProvider).getShortestPath(departureHubId, arrivalHubId);
    }

    private HubRoutePathResponse pathResponse(UUID departureHubId, UUID arrivalHubId) {
        return HubRoutePathResponse.builder()
                .totalDistanceMeters(100_000L)
                .totalDurationSeconds(3_600L)
                .totalDistanceKm(100.0)
                .totalDurationMin(60L)
                .segments(List.of(HubRoutePathResponse.HubRoutePathSegment.builder()
                        .sequence(1)
                        .fromHubId(departureHubId)
                        .toHubId(arrivalHubId)
                        .distanceMeters(100_000)
                        .durationSeconds(3_600)
                        .distanceKm(100.0)
                        .durationMin(60)
                        .build()))
                .build();
    }
}