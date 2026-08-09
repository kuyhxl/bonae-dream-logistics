package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.request.ReqCreateDeliveryDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResCancelDeliveryDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResCreateDeliveryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
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
    @DisplayName("배송 생성에 성공한다")
    void createDelivery_success() throws Exception {
        ReqCreateDeliveryDto reqDto = createRequest();
        when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResCreateDeliveryDto result = deliveryService.createDelivery(reqDto);

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).saveAndFlush(captor.capture());

        Delivery savedDelivery = captor.getValue();
        assertThat(savedDelivery.getOrderId()).isEqualTo(reqDto.getOrderId());
        assertThat(savedDelivery.getOriginHubId()).isEqualTo(reqDto.getOriginHubId());
        assertThat(savedDelivery.getDestinationHubId()).isEqualTo(reqDto.getDestinationHubId());
        assertThat(savedDelivery.getReceiverCompanyId()).isEqualTo(reqDto.getReceiverCompanyId());
        assertThat(savedDelivery.getReceiverName()).isEqualTo("홍길동");
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.READY);

        assertThat(result.getDeliveryId()).isEqualTo(savedDelivery.getId());
        assertThat(result.getOrderId()).isEqualTo(reqDto.getOrderId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.READY);
    }

    @Test
    @DisplayName("배송 취소에 성공한다")
    void cancelDelivery_success() {
        Delivery delivery = Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "홍길동",
                "hong123",
                "서울시 강남구 테헤란로 1"
        );
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        ResCancelDeliveryDto result = deliveryService.cancelDelivery(delivery.getId());

        verify(deliveryRepository).flush();
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 완료 상태는 취소할 수 없다")
    void cancelDelivery_completed() throws Exception {
        Delivery delivery = Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "홍길동",
                "hong123",
                "서울시 강남구 테헤란로 1"
        );
        setField(delivery, "status", DeliveryStatus.DELIVERED);
        setField(delivery, "completedAt", LocalDateTime.now());
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_ALREADY_COMPLETED);
    }

    private ReqCreateDeliveryDto createRequest() throws Exception {
        ReqCreateDeliveryDto reqDto = new ReqCreateDeliveryDto();
        setField(reqDto, "orderId", UUID.randomUUID());
        setField(reqDto, "originHubId", UUID.randomUUID());
        setField(reqDto, "destinationHubId", UUID.randomUUID());
        setField(reqDto, "receiverCompanyId", UUID.randomUUID());
        setField(reqDto, "receiverName", "  홍길동  ");
        setField(reqDto, "receiverSlackId", "  hong123  ");
        setField(reqDto, "deliveryAddress", "  서울시 강남구 테헤란로 1  ");
        return reqDto;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
