package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.UUID;

public record InventoryUpdateResponseDto(
        UUID orderId,
        UUID inventoryId,
        Integer beforeQuantity,
        Integer changedQuantity,
        Integer afterQuantity,
        String type,
        String updatedAt
) {}