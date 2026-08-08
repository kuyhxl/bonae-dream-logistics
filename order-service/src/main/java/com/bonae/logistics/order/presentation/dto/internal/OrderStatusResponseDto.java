package com.bonae.logistics.order.presentation.dto.internal;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusResponseDto(
        UUID orderId,
        String status,
        LocalDateTime updatedAt
) {
    public static OrderStatusResponseDto of(UUID orderId, String status, LocalDateTime updatedAt) {
        return new OrderStatusResponseDto(orderId, status, updatedAt);
    }
}