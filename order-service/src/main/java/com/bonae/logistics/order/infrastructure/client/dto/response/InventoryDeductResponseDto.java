package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.UUID;

// 재고 차감 응답 Dto
public record InventoryDeductResponseDto(
        UUID productId,
        Integer remainingStock
) {
}
