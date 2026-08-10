package com.bonae.logistics.order.infrastructure.client.dto;

import java.util.UUID;

//배송 생성 요청 Dto
public record DeliveryCreateRequestDto(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        String productInfo,
        String requestNote,  // Order의 remarks와 매핑됨
        String deliveryAddress
) {
}
