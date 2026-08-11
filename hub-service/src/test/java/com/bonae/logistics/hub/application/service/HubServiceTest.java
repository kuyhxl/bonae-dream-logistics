package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @Mock
    private HubRepository hubRepository;

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Mock
    private AuditorAware<String> auditorAware;

    @InjectMocks
    private HubService hubService;

    private Hub createHub(String name, String address, double latitude, double longitude) {
        Hub hub = Hub.create(name, address, latitude, longitude);
        ReflectionTestUtils.setField(hub, "id", UUID.randomUUID());
        return hub;
    }

    private HubUpdateRequest updateRequestWithName(String name) {
        HubUpdateRequest request = new HubUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }

    @Test
    @DisplayName("활성 허브를 조회하면 허브 상세 응답을 반환한다")
    void returnsHubDetailWhenHubIsActive() {
        Hub hub = createHub("서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.4742027808565, 127.123621185562);
        when(hubRepository.findByIdAndDeletedAtIsNull(hub.getId())).thenReturn(Optional.of(hub));
        HubDetailResponse result = hubService.getHubDetail(hub.getId());

        assertThat(result.getHubId()).isEqualTo(hub.getId());
        assertThat(result.getName()).isEqualTo(hub.getName());
    }

    @Test
    @DisplayName("활성 허브가 존재하지 않으면 HUB_NOT_FOUND 예외가 발생한다")
    void throwsHubNotFoundWhenActiveHubDoesNotExist() {
        UUID hubId = UUID.randomUUID();
        when(hubRepository.findByIdAndDeletedAtIsNull(hubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hubService.getHubDetail(hubId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.HUB_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("허브 수정 요청에 포함된 필드만 변경한다")
    void updatesOnlyProvidedHubFields() {
        Hub hub = createHub("서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.4742027808565, 127.123621185562);
        String originalAddress = hub.getAddress();
        HubUpdateRequest request = updateRequestWithName("서울특별시 북부 센터");

        when(hubRepository.findByIdAndDeletedAtIsNull(hub.getId())).thenReturn(Optional.of(hub));
        when(hubRepository.existsByNameAndDeletedAtIsNullAndIdNot(request.getName(), hub.getId()))
                .thenReturn(false);

        hubService.update(hub.getId(), request);

        assertThat(hub.getName()).isEqualTo("서울특별시 북부 센터");
        assertThat(hub.getAddress()).isEqualTo(originalAddress);
    }

    @Test
    @DisplayName("허브를 삭제하면 허브와 연결된 이동정보를 함께 논리 삭제한다")
    void softDeletesHubAndRelatedRoutes() {
        Hub hub = createHub("서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.4742027808565, 127.123621185562);
        Hub otherHub = createHub("경기 남부 센터", "경기도 이천시 덕평로 257-21", 37.1896213142136, 127.375050006958);
        HubRoute route = HubRoute.create(hub, otherHub);

        when(hubRepository.findByIdAndDeletedAtIsNull(hub.getId())).thenReturn(Optional.of(hub));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("master03"));
        when(hubRouteRepository.findAllActiveByHubId(hub.getId())).thenReturn(List.of(route));

        hubService.delete(hub.getId());

        assertThat(hub.isDeleted()).isTrue();
        assertThat(hub.getDeletedBy()).isEqualTo("master03");
        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedBy()).isEqualTo("master03");
    }
}