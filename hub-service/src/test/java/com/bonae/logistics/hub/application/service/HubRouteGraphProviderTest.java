package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubRouteGraphProviderTest {

    @Mock
    private HubRouteRepository hubRouteRepository;

    @InjectMocks
    private HubRouteGraphProvider hubRouteGraphProvider;

    private Hub createHub(String name, String address, double latitude, double longitude) {
        Hub hub = Hub.create(name, address, latitude, longitude);
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        return hub;
    }

    @Test
    @DisplayName("활성 이동정보를 경로 탐색용 불변 간선으로 변환한다")
    void returnsActiveRoutesAsEdges() {
        Hub departureHub = createHub("서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.4742027808565, 127.123621185562);
        Hub arrivalHub = createHub("경기 남부 센터", "경기도 이천시 덕평로 257-21", 37.1896213142136, 127.375050006958);
        HubRoute route = HubRoute.create(departureHub, arrivalHub);
        route.update(150_000, 7_200);

        when(hubRouteRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(route));

        List<HubRouteEdge> result = hubRouteGraphProvider.getActiveRoutes();

        assertThat(result).containsExactly(
                new HubRouteEdge(
                        departureHub.getId(),
                        arrivalHub.getId(),
                        150_000,
                        7_200
                )
        );
    }
}