package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("배송 단건 조회에 성공한다")
    void getDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.getDelivery(delivery.getId());

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getOrderId()).isEqualTo(delivery.getOrderId());
        assertThat(result.getStatus()).isEqualTo(delivery.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 배송 조회 시 예외가 발생한다")
    void getDelivery_notFound() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId))
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

        PageResponseDto<DeliveryListItemResponse> result = deliveryService.getDeliveries(pageRequestDto);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(delivery.getStatus());
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
}
