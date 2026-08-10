package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.util.UUID;

//재고 복원 요청 Dto
public record InventoryRestoreRequestDto(
        UUID orderId,
        Integer quantity
) {
}
