package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerSearchRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryManagerResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerServiceTest {

    private static final String HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX =
            "uk_p_delivery_managers_active_hub_delivery_sequence";

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private CurrentAuditorProvider currentAuditorProvider;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    @Test
    @DisplayName("배송 담당자 생성 시 요청 ID를 그대로 사용한다")
    void createDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManagerCreateRequest request = DeliveryManagerCreateRequest.builder()
                .deliveryManagerId(deliveryManagerId)
                .hubId(null)
                .managerType(ManagerType.HUB_DELIVERY)
                .deliverySequence(0)
                .build();

        DeliveryManager savedDeliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                0
        );

        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class))).thenReturn(savedDeliveryManager);

        DeliveryManagerResponse response = deliveryManagerService.createDeliveryManager(request);

        assertThat(response.getDeliveryManagerId()).isEqualTo(deliveryManagerId);
        assertThat(response.getManagerType()).isEqualTo(ManagerType.HUB_DELIVERY);
        assertThat(response.getDeliverySequence()).isEqualTo(0);
        verify(deliveryManagerRepository).saveAndFlush(any(DeliveryManager.class));
    }

    @Test
    @DisplayName("배송 담당자 순번 중복은 비즈니스 예외로 변환한다")
    void createDeliveryManager_duplicateSequence() {
        DeliveryManagerCreateRequest request = DeliveryManagerCreateRequest.builder()
                .deliveryManagerId(UUID.randomUUID())
                .hubId(null)
                .managerType(ManagerType.HUB_DELIVERY)
                .deliverySequence(0)
                .build();

        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
                .thenThrow(duplicateKeyException(HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX));

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_SEQUENCE_DUPLICATED);
    }

    @Test
    @DisplayName("MASTER는 배송 담당자 단건을 조회할 수 있다")
    void getDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                1
        );

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));

        DeliveryManagerResponse response = deliveryManagerService.getDeliveryManager(
                deliveryManagerId,
                UserRole.MASTER,
                null
        );

        assertThat(response.getDeliveryManagerId()).isEqualTo(deliveryManagerId);
        verify(deliveryManagerRepository).findByIdAndDeletedAtIsNull(deliveryManagerId);
    }

    @Test
    @DisplayName("배송 담당자가 없으면 예외가 발생한다")
    void getDeliveryManager_notFound() {
        UUID deliveryManagerId = UUID.randomUUID();
        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryManagerService.getDeliveryManager(
                deliveryManagerId,
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_NOT_FOUND);
    }

    @Test
    @DisplayName("허브 관리자는 다른 허브 담당자를 조회할 수 없다")
    void getDeliveryManager_hubManagerForbiddenWhenDifferentHub() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                UUID.randomUUID(),
                ManagerType.COMPANY_DELIVERY,
                1
        );

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));
        when(userClient.getUserInfo("hub-manager"))
                .thenReturn(createUserInfo(UUID.randomUUID(), "hub-manager", UUID.randomUUID()));

        assertThatThrownBy(() -> deliveryManagerService.getDeliveryManager(
                deliveryManagerId,
                UserRole.HUB_MANAGER,
                "hub-manager"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("배송 담당자는 본인 정보만 조회할 수 있다")
    void getDeliveryManager_deliveryManagerCanViewSelf() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                1
        );

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createUserInfo(deliveryManagerId, "delivery-manager", null));

        DeliveryManagerResponse response = deliveryManagerService.getDeliveryManager(
                deliveryManagerId,
                UserRole.DELIVERY_MANAGER,
                "delivery-manager"
        );

        assertThat(response.getDeliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("배송 담당자 목록 조회 결과를 공통 응답으로 반환한다")
    void getDeliveryManagers_success() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        DeliveryManager deliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                null,
                ManagerType.HUB_DELIVERY,
                2
        );
        Page<DeliveryManager> page = new PageImpl<>(List.of(deliveryManager), pageRequestDto.toPageable(), 1);

        when(deliveryManagerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponseDto<DeliveryManagerResponse> response =
                deliveryManagerService.getDeliveryManagers(pageRequestDto, UserRole.MASTER, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDeliverySequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송 담당자 검색 조건을 적용한 결과를 반환한다")
    void searchDeliveryManagers_withSearchFilters() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        DeliveryManagerSearchRequest searchRequest = new DeliveryManagerSearchRequest();
        UUID hubId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                hubId,
                ManagerType.COMPANY_DELIVERY,
                5
        );
        Page<DeliveryManager> page = new PageImpl<>(List.of(deliveryManager), pageRequestDto.toPageable(), 1);

        setField(searchRequest, "hubId", hubId);
        setField(searchRequest, "managerType", ManagerType.COMPANY_DELIVERY);
        setField(searchRequest, "deliverySequence", 5);

        when(deliveryManagerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponseDto<DeliveryManagerResponse> response =
                deliveryManagerService.searchDeliveryManagers(
                        pageRequestDto,
                        searchRequest,
                        UserRole.MASTER,
                        null
                );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getHubId()).isEqualTo(hubId);
        assertThat(response.getContent().get(0).getManagerType()).isEqualTo(ManagerType.COMPANY_DELIVERY);
        assertThat(response.getContent().get(0).getDeliverySequence()).isEqualTo(5);
    }

    @Test
    @DisplayName("배송 담당자 권한 검색은 본인 정보로 제한된다")
    void searchDeliveryManagers_deliveryManagerScopedToSelf() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        DeliveryManagerSearchRequest searchRequest = new DeliveryManagerSearchRequest();
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                2
        );
        Page<DeliveryManager> page = new PageImpl<>(List.of(deliveryManager), pageRequestDto.toPageable(), 1);

        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createUserInfo(deliveryManagerId, "delivery-manager", null));
        when(deliveryManagerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponseDto<DeliveryManagerResponse> response = deliveryManagerService.searchDeliveryManagers(
                pageRequestDto,
                searchRequest,
                UserRole.DELIVERY_MANAGER,
                "delivery-manager"
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDeliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("배송 담당자 수정 성공")
    void updateDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                0
        );
        DeliveryManagerUpdateRequest request = DeliveryManagerUpdateRequest.builder()
                .hubId(UUID.randomUUID())
                .managerType(ManagerType.COMPANY_DELIVERY)
                .deliverySequence(3)
                .build();

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));

        DeliveryManagerResponse response = deliveryManagerService.updateDeliveryManager(deliveryManagerId, request);

        assertThat(response.getManagerType()).isEqualTo(ManagerType.COMPANY_DELIVERY);
        assertThat(response.getHubId()).isEqualTo(request.getHubId());
        assertThat(response.getDeliverySequence()).isEqualTo(3);
        verify(deliveryManagerRepository).flush();
    }

    @Test
    @DisplayName("배송 담당자 삭제 시 삭제자를 기록한다")
    void deleteDeliveryManager_recordsAuditor() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                0
        );

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));
        when(currentAuditorProvider.getCurrentAuditorOrSystem()).thenReturn("tester");

        deliveryManagerService.deleteDeliveryManager(deliveryManagerId);

        assertThat(deliveryManager.isDeleted()).isTrue();
        assertThat(deliveryManager.getDeletedBy()).isEqualTo("tester");
    }

    @Test
    @DisplayName("삭제 대상 배송 담당자가 없으면 삭제자를 조회하지 않는다")
    void deleteDeliveryManager_notFound() {
        UUID deliveryManagerId = UUID.randomUUID();
        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryManagerService.deleteDeliveryManager(deliveryManagerId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_NOT_FOUND);

        verify(currentAuditorProvider, never()).getCurrentAuditorOrSystem();
    }

    private DataIntegrityViolationException duplicateKeyException(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"" + constraintName + "\""),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate key", cause);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private UserInfoClientResponse createUserInfo(UUID id, String username, UUID hubId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .hubId(hubId)
                .build();
    }
}
