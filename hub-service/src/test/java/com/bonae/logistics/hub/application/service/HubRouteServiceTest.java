package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteCreateRequest;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubRouteServiceTest {

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private AuditorAware<String> auditorAware;

    @InjectMocks
    private HubRouteService hubRouteService;

    // 이동정보 생성 시 Haversine 거리를 계산하므로 출발/도착 허브에 서로 다른 좌표를 사용한다.
    private Hub createHub(String name, String address, double latitude, double longitude) {
        Hub hub = Hub.create(name, address, latitude, longitude);
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        return hub;
    }

    private Hub createSeoulHub() {
        return createHub(
                "서울특별시 센터",
                "서울특별시 송파구 송파대로 55",
                37.4742027808565,
                127.123621185562
        );
    }

    private Hub createGyeonggiHub() {
        return createHub(
                "경기 남부 센터",
                "경기도 이천시 덕평로 257-21",
                37.1896213142136,
                127.375050006958
        );
    }

    private HubRouteCreateRequest createRequest(UUID departureHubId, UUID arrivalHubId) {
        HubRouteCreateRequest request = new HubRouteCreateRequest();
        ReflectionTestUtils.setField(request, "departureHubId", departureHubId);
        ReflectionTestUtils.setField(request, "arrivalHubId", arrivalHubId);
        return request;
    }

    private HubRouteUpdateRequest updateRequest(int distanceMeters, int durationSeconds) {
        HubRouteUpdateRequest request = new HubRouteUpdateRequest();
        ReflectionTestUtils.setField(request, "distanceMeters", distanceMeters);
        ReflectionTestUtils.setField(request, "durationSeconds", durationSeconds);
        return request;
    }

    @Test
    @DisplayName("활성 허브 사이의 이동정보를 생성한다")
    void createsHubRoute() {
        Hub departureHub = createSeoulHub();
        Hub arrivalHub = createGyeonggiHub();
        HubRouteCreateRequest request = createRequest(departureHub.getId(), arrivalHub.getId());

        when(hubRepository.findByIdAndDeletedAtIsNull(departureHub.getId()))
                .thenReturn(Optional.of(departureHub));
        when(hubRepository.findByIdAndDeletedAtIsNull(arrivalHub.getId()))
                .thenReturn(Optional.of(arrivalHub));
        when(hubRouteRepository.existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
                departureHub.getId(), arrivalHub.getId()
        )).thenReturn(false);

        HubRouteDetailResponse result = hubRouteService.create(request);

        assertThat(result.getDepartureHubId()).isEqualTo(departureHub.getId());
        assertThat(result.getArrivalHubId()).isEqualTo(arrivalHub.getId());
        assertThat(result.getDistanceMeters()).isPositive();
        assertThat(result.getDurationSeconds()).isPositive();

        verify(hubRouteRepository).saveAndFlush(any(HubRoute.class));
    }

    @Test
    @DisplayName("활성 상태의 동일 방향 이동정보가 존재하면 중복 예외가 발생한다")
    void throwsDuplicatedWhenRouteAlreadyExists() {
        Hub departureHub = createSeoulHub();
        Hub arrivalHub = createGyeonggiHub();
        HubRouteCreateRequest request = createRequest(departureHub.getId(), arrivalHub.getId());

        when(hubRepository.findByIdAndDeletedAtIsNull(departureHub.getId()))
                .thenReturn(Optional.of(departureHub));
        when(hubRepository.findByIdAndDeletedAtIsNull(arrivalHub.getId()))
                .thenReturn(Optional.of(arrivalHub));
        when(hubRouteRepository.existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
                departureHub.getId(), arrivalHub.getId()
        )).thenReturn(true);

        assertThatThrownBy(() -> hubRouteService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.HUB_ROUTE_DUPLICATED)
                );

        verify(hubRouteRepository, never()).saveAndFlush(any(HubRoute.class));
    }

    @Test
    @DisplayName("이동정보의 거리와 소요시간을 수정한다")
    void updatesHubRoute() {
        Hub departureHub = createSeoulHub();
        Hub arrivalHub = createGyeonggiHub();
        HubRoute route = HubRoute.create(departureHub, arrivalHub);
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());

        HubRouteUpdateRequest request = updateRequest(150_000, 7_200);

        when(hubRouteRepository.findByIdAndDeletedAtIsNull(route.getId()))
                .thenReturn(Optional.of(route));

        HubRouteDetailResponse result = hubRouteService.update(route.getId(), request);

        assertThat(route.getDistanceMeters()).isEqualTo(150_000);
        assertThat(route.getDurationSeconds()).isEqualTo(7_200);
        assertThat(result.getDistanceMeters()).isEqualTo(150_000);
        assertThat(result.getDurationSeconds()).isEqualTo(7_200);
    }

    @Test
    @DisplayName("이동정보를 논리 삭제하고 삭제자를 기록한다")
    void softDeletesHubRoute() {
        Hub departureHub = createSeoulHub();
        Hub arrivalHub = createGyeonggiHub();
        HubRoute route = HubRoute.create(departureHub, arrivalHub);
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());

        when(hubRouteRepository.findByIdAndDeletedAtIsNull(route.getId()))
                .thenReturn(Optional.of(route));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("master03"));

        hubRouteService.delete(route.getId());

        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedAt()).isNotNull();
        assertThat(route.getDeletedBy()).isEqualTo("master03");
        verify(hubRouteRepository, never()).delete(any(HubRoute.class));
    }
}