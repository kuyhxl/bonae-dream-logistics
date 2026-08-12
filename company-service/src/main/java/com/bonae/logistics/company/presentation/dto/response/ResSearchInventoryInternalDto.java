package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Inventory;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ResSearchInventoryInternalDto {
    private UUID inventoryId;
    private UUID productId;
    private UUID hubId;
    private Integer quantity;

    public static ResSearchInventoryInternalDto from(Inventory inventory) {
        return ResSearchInventoryInternalDto.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProduct().getId())
                .hubId(inventory.getHubId())
                .quantity(inventory.getQuantity())
                .build();
    }
}