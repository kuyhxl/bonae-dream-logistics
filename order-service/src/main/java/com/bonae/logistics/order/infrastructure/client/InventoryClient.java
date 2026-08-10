package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.request.InventoryDeductRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryDeductResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.request.InventoryRestoreRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryRestoreResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "company-service", contextId = "inventoryClient")
public interface InventoryClient {

    @PostMapping("/api/internal/products/{productId}/inventories/deduct")
    InventoryDeductResponseDto deductStock(
            @PathVariable UUID productId,
            @RequestBody InventoryDeductRequestDto request
            );

    @PostMapping("/api/internal/products/{productId}/stock/restore")
    InventoryRestoreResponseDto restoreStock(
            @PathVariable UUID productId,
            @RequestBody InventoryRestoreRequestDto request
    );
}
