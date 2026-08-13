package com.bonae.logistics.company.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResUpdateInventoryDto {
    private UUID orderId;
    private UUID inventoryId;
    private Integer beforeQuantity;
    private Integer changedQuantity;
    private Integer afterQuantity;
    private String type;
    private LocalDateTime updatedAt;

    public static ResUpdateInventoryDto of(UUID orderId, UUID inventoryId, Integer beforeQuantity,
                                            Integer changedQuantity, Integer afterQuantity,
                                            String type, LocalDateTime updatedAt) {
        return ResUpdateInventoryDto.builder()
                .orderId(orderId)
                .inventoryId(inventoryId)
                .beforeQuantity(beforeQuantity)
                .changedQuantity(changedQuantity)
                .afterQuantity(afterQuantity)
                .type(type)
                .updatedAt(updatedAt)
                .build();
    }
}
