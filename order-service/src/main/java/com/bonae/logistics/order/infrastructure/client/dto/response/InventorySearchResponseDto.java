package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.List;
import java.util.UUID;

public record InventorySearchResponseDto(
        List<InventoryItem> content
) {
    public record InventoryItem(
            UUID inventoryId,
            UUID productId,
            UUID hubId,
            Integer quantity
    ) {}
}
