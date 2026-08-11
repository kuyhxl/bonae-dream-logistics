package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.UUID;

public record DeliveryCancelResponseDto(
        UUID deliveryId,
        String status
) {
}
