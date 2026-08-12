package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerCreateRequest;
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

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    @Test
    @DisplayName("배송 담당자 생성 시 요청 ID를 그대로 사용한다")
    void createDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManagerCreateRequest request = DeliveryManagerCreateRequest.builder()
                .deliveryManagerId(deliveryManagerId)
                .hubId(hubId)
                .managerType(ManagerType.HUB_DELIVERY)
                .deliverySequence(0)
                .build();

        DeliveryManager savedDeliveryManager = DeliveryManager.create(
                deliveryManagerId,
                hubId,
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
                .hubId(UUID.randomUUID())
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
    @DisplayName("배송 담당자 단건 조회 성공")
    void getDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                UUID.randomUUID(),
                ManagerType.HUB_DELIVERY,
                1
        );

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));

        DeliveryManagerResponse response = deliveryManagerService.getDeliveryManager(deliveryManagerId);

        assertThat(response.getDeliveryManagerId()).isEqualTo(deliveryManagerId);
        verify(deliveryManagerRepository).findByIdAndDeletedAtIsNull(deliveryManagerId);
    }

    @Test
    @DisplayName("배송 담당자가 없으면 예외 발생")
    void getDeliveryManager_notFound() {
        UUID deliveryManagerId = UUID.randomUUID();
        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryManagerService.getDeliveryManager(deliveryManagerId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_NOT_FOUND);
    }

    @Test
    @DisplayName("배송 담당자 목록 조회 결과를 공통 응답으로 반환한다")
    void getDeliveryManagers_success() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        DeliveryManager deliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ManagerType.HUB_DELIVERY,
                2
        );
        Page<DeliveryManager> page = new PageImpl<>(List.of(deliveryManager), pageRequestDto.toPageable(), 1);

        when(deliveryManagerRepository.findAllByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        PageResponseDto<DeliveryManagerResponse> response = deliveryManagerService.getDeliveryManagers(pageRequestDto);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDeliverySequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("배송 담당자 수정 성공")
    void updateDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                UUID.randomUUID(),
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
                UUID.randomUUID(),
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
}
