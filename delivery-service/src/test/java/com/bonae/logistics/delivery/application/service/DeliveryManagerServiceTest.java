package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.presentation.dto.request.ReqCreateDeliveryManagerDto;
import com.bonae.logistics.delivery.presentation.dto.request.ReqUpdateDeliveryManagerDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResDeliveryManagerDto;
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

    private static final String HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX = "uk_p_delivery_managers_active_hub_delivery_sequence";

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private CurrentAuditorProvider currentAuditorProvider;

    @InjectMocks
    private DeliveryManagerService deliveryManagerService;

    @Test
    @DisplayName("createDeliveryManager_정상 요청이면 배송 담당자를 생성한다")
    void createDeliveryManager_success() {
        ReqCreateDeliveryManagerDto reqDto = ReqCreateDeliveryManagerDto.builder()
                .managerType(ManagerType.HUB_DELIVERY)
                .deliverySequence(0)
                .build();

        DeliveryManager savedDeliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                null,
                ManagerType.HUB_DELIVERY,
                0
        );

        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class))).thenReturn(savedDeliveryManager);

        ResDeliveryManagerDto resDto = deliveryManagerService.createDeliveryManager(reqDto);

        assertThat(resDto.getDeliveryManagerId()).isEqualTo(savedDeliveryManager.getId());
        assertThat(resDto.getManagerType()).isEqualTo(ManagerType.HUB_DELIVERY);
        assertThat(resDto.getDeliverySequence()).isEqualTo(0);
        verify(deliveryManagerRepository).saveAndFlush(any(DeliveryManager.class));
    }

    @Test
    @DisplayName("createDeliveryManager_순번 유니크 충돌이면 비즈니스 예외로 변환한다")
    void createDeliveryManager_duplicateSequence() {
        ReqCreateDeliveryManagerDto reqDto = ReqCreateDeliveryManagerDto.builder()
                .managerType(ManagerType.HUB_DELIVERY)
                .deliverySequence(0)
                .build();

        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
                .thenThrow(duplicateKeyException(HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX));

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_SEQUENCE_DUPLICATED);
    }

    @Test
    @DisplayName("getDeliveryManager_삭제되지 않은 배송 담당자를 조회한다")
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

        ResDeliveryManagerDto resDto = deliveryManagerService.getDeliveryManager(deliveryManagerId);

        assertThat(resDto.getDeliveryManagerId()).isEqualTo(deliveryManagerId);
        verify(deliveryManagerRepository).findByIdAndDeletedAtIsNull(deliveryManagerId);
    }

    @Test
    @DisplayName("getDeliveryManager_존재하지 않으면 예외가 발생한다")
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
    @DisplayName("getDeliveryManagers_페이지 조회 결과를 공통 응답으로 반환한다")
    void getDeliveryManagers_success() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        DeliveryManager deliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                null,
                ManagerType.HUB_DELIVERY,
                2
        );
        Page<DeliveryManager> page = new PageImpl<>(List.of(deliveryManager), pageRequestDto.toPageable(), 1);

        when(deliveryManagerRepository.findAllByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        PageResponseDto<ResDeliveryManagerDto> response = deliveryManagerService.getDeliveryManagers(pageRequestDto);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getDeliverySequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateDeliveryManager_정상 요청이면 배송 담당자 정보를 수정한다")
    void updateDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        DeliveryManager deliveryManager = DeliveryManager.create(
                deliveryManagerId,
                null,
                ManagerType.HUB_DELIVERY,
                0
        );
        ReqUpdateDeliveryManagerDto reqDto = ReqUpdateDeliveryManagerDto.builder()
                .hubId(UUID.randomUUID())
                .managerType(ManagerType.COMPANY_DELIVERY)
                .deliverySequence(3)
                .build();

        when(deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId))
                .thenReturn(Optional.of(deliveryManager));

        ResDeliveryManagerDto response = deliveryManagerService.updateDeliveryManager(deliveryManagerId, reqDto);

        assertThat(response.getManagerType()).isEqualTo(ManagerType.COMPANY_DELIVERY);
        assertThat(response.getHubId()).isEqualTo(reqDto.getHubId());
        assertThat(response.getDeliverySequence()).isEqualTo(3);
        verify(deliveryManagerRepository).flush();
    }

    @Test
    @DisplayName("deleteDeliveryManager_현재 요청자를 삭제자로 기록한다")
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
    @DisplayName("deleteDeliveryManager_존재하지 않으면 삭제를 수행하지 않는다")
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
