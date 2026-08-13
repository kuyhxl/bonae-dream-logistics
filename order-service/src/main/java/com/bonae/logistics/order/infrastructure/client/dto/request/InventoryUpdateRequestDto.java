package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.util.UUID;

public record InventoryUpdateRequestDto(
        UUID orderId,
        Integer quantity,
        String type
) {}

