package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.util.UUID;

//재고 차감 요청 Dto
public record InventoryDeductRequestDto(
        UUID orderId,
        Integer quantity
) {
}
