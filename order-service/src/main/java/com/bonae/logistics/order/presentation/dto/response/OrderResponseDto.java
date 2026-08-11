package com.bonae.logistics.order.presentation.dto.response;

import com.bonae.logistics.order.domain.entity.Order;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OrderResponseDto(
        UUID id,
        UUID requesterCompanyId,
        UUID receiverCompanyId,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDateTime dueDate,
        String remarks,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .requesterCompanyId(order.getRequesterCompanyId())
                .receiverCompanyId(order.getReceiverCompanyId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .dueDate(order.getDueDate())
                .remarks(order.getRemarks())
                .status(order.getStatus().name())
                .createdBy(order.getCreatedBy())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .updatedBy(order.getUpdatedBy())
                .build();
    }
}