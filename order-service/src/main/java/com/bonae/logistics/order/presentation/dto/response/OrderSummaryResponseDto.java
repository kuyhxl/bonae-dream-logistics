package com.bonae.logistics.order.presentation.dto.response;

import com.bonae.logistics.order.domain.entity.Order;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OrderSummaryResponseDto(
        UUID id,
        UUID receiverCompanyId,
        String productName,
        Integer quantity,
        String status,
        LocalDateTime dueDate,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponseDto from(Order order) {
        return OrderSummaryResponseDto.builder()
                .id(order.getId())
                .receiverCompanyId(order.getReceiverCompanyId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .status(order.getStatus().name())
                .dueDate(order.getDueDate())
                .createdAt(order.getCreatedAt())
                .build();
    }
}