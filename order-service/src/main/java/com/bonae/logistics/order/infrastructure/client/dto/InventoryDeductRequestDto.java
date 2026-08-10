package com.bonae.logistics.order.infrastructure.client.dto;

import java.util.UUID;

//재고 차감 요청 Dto
public record InventoryDeductRequestDto(
        UUID orderId,
        Integer quantity
) {
}
