package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.UUID;

//재고 복원 응답 Dto
public record InventoryRestoreResponseDto(
        UUID productId,
        Integer remainingStock
) {
}
