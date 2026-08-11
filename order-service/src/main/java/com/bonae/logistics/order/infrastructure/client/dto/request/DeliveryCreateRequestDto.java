package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.util.UUID;

//배송 생성 요청 Dto
public record DeliveryCreateRequestDto(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        String receiverUsername,
        String productInfo,
        String requestNote  // Order의 remarks와 매핑됨
) {
}
