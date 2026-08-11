package com.bonae.logistics.order.infrastructure.client.dto;

import java.util.UUID;

// 배송 생성 요청 DTO
public record DeliveryCreateRequestDto(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        String receiverUsername,
        String productInfo,
        String requestNote
) {
}
