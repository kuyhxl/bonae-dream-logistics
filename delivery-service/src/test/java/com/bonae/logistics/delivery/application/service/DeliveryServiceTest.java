package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("배송 생성은 오케스트레이션 구현 전까지 스켈레톤으로 유지한다")
    void createDelivery_skeleton() throws Exception {
        DeliveryCreateRequest reqDto = createRequest();

        assertThatThrownBy(() -> deliveryService.createDelivery(reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("배송 단건 조회에 성공한다")
    void getDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.getDelivery(delivery.getId(), UserRole.MASTER, null);

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getOrderId()).isEqualTo(delivery.getOrderId());
        assertThat(result.getStatus()).isEqualTo(delivery.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 배송 조회 시 예외가 발생한다")
    void getDelivery_notFound() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, UserRole.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_NOT_FOUND);
    }

    @Test
    @DisplayName("배송 목록을 페이지 형태로 조회한다")
    void getDeliveries_success() {
        Delivery delivery = createDelivery();
        PageRequestDto pageRequestDto = new PageRequestDto();
        when(deliveryRepository.findAllByDeletedAtIsNull(any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        PageResponseDto<DeliveryListItemResponse> result = deliveryService.getDeliveries(pageRequestDto, UserRole.MASTER, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(delivery.getStatus());
    }

    @Test
    @DisplayName("업체 담당자는 자기 업체 배송만 단건 조회한다")
    void getDelivery_companyManagerScoped() {
        Delivery delivery = createDelivery();
        UUID companyId = delivery.getReceiverCompanyId();
        when(deliveryRepository.findByIdAndReceiverCompanyIdAndDeletedAtIsNull(delivery.getId(), companyId))
                .thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.getDelivery(delivery.getId(), UserRole.COMPANY_MANAGER, companyId);

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("업체 담당자는 companyId 헤더가 없으면 목록 조회할 수 없다")
    void getDeliveries_companyManagerWithoutCompanyId() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> deliveryService.getDeliveries(pageRequestDto, UserRole.COMPANY_MANAGER, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("업체 담당자는 자기 업체 배송만 목록 조회한다")
    void getDeliveries_companyManagerScoped() {
        Delivery delivery = createDelivery();
        PageRequestDto pageRequestDto = new PageRequestDto();
        UUID companyId = delivery.getReceiverCompanyId();
        when(deliveryRepository.findAllByReceiverCompanyIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        PageResponseDto<DeliveryListItemResponse> result = deliveryService.getDeliveries(pageRequestDto, UserRole.COMPANY_MANAGER, companyId);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("배송 취소에 성공한다")
    void cancelDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result = deliveryService.cancelDelivery(delivery.getId());

        verify(deliveryRepository).flush();
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 완료 상태는 취소할 수 없다")
    void cancelDelivery_completed() throws Exception {
        Delivery delivery = createDelivery();
        setField(delivery, "status", DeliveryStatus.DELIVERED);
        setField(delivery, "completedAt", LocalDateTime.now());
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_ALREADY_COMPLETED);
    }

    private DeliveryCreateRequest createRequest() throws Exception {
        DeliveryCreateRequest reqDto = new DeliveryCreateRequest();
        setField(reqDto, "orderId", UUID.randomUUID());
        setField(reqDto, "supplierCompanyId", UUID.randomUUID());
        setField(reqDto, "receiverCompanyId", UUID.randomUUID());
        setField(reqDto, "productInfo", "  마른 오징어 50박스  ");
        setField(reqDto, "requestNote", "  12월 12일 3시까지 부탁드립니다.  ");
        return reqDto;
    }

    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "홍길동",
                "hong123",
                "서울시 강남구 테헤란로 1"
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
