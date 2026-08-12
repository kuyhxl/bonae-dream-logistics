package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubRoutePathProviderTest {

    @Mock
    private HubRouteGraphProvider hubRouteGraphProvider;

    @Mock
    private HubRoutePathFinder hubRoutePathFinder;

    @InjectMocks
    private HubRoutePathProvider hubRoutePathProvider;

    @Test
    @DisplayName("활성 간선으로 최단경로를 계산하고 응답으로 변환한다")
    void returnsShortestPathResponse() {
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        HubRouteEdge edge = new HubRouteEdge(departureHubId, arrivalHubId, 100_000, 3_600);
        List<HubRouteEdge> activeRoutes = List.of(edge);

        when(hubRouteGraphProvider.getActiveRoutes()).thenReturn(activeRoutes);
        when(hubRoutePathFinder.findShortestPath(activeRoutes, departureHubId, arrivalHubId)).thenReturn(List.of(edge));

        HubRoutePathResponse result = hubRoutePathProvider.getShortestPath(departureHubId, arrivalHubId);

        assertThat(result.getTotalDistanceMeters()).isEqualTo(100_000L);
        assertThat(result.getTotalDurationSeconds()).isEqualTo(3_600L);
        assertThat(result.getSegments()).hasSize(1);

        verify(hubRouteGraphProvider).getActiveRoutes();
        verify(hubRoutePathFinder).findShortestPath(activeRoutes, departureHubId, arrivalHubId);
    }

    @Test
    @DisplayName("경로 총합이 int 범위를 넘어도 long 값으로 응답한다")
    void returnsLongPathTotals() {
        UUID hubA = UUID.randomUUID();
        UUID hubB = UUID.randomUUID();
        UUID hubC = UUID.randomUUID();

        HubRouteEdge edgeAB = new HubRouteEdge(hubA, hubB, 1_500_000_000, 1_500_000_000);
        HubRouteEdge edgeBC = new HubRouteEdge(hubB, hubC, 1_500_000_000, 1_500_000_000);
        List<HubRouteEdge> activeRoutes = List.of(edgeAB, edgeBC);
        List<HubRouteEdge> path = List.of(edgeAB, edgeBC);

        when(hubRouteGraphProvider.getActiveRoutes()).thenReturn(activeRoutes);
        when(hubRoutePathFinder.findShortestPath(activeRoutes, hubA, hubC)).thenReturn(path);

        HubRoutePathResponse result = hubRoutePathProvider.getShortestPath(hubA, hubC);

        assertThat(result.getTotalDistanceMeters()).isEqualTo(3_000_000_000L);
        assertThat(result.getTotalDurationSeconds()).isEqualTo(3_000_000_000L);
        assertThat(result.getTotalDurationMin()).isEqualTo(50_000_000L);
    }
}