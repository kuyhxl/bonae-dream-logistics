package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.request.InventoryUpdateRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventorySearchResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryUpdateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "company-service", contextId = "inventoryClient")
public interface InventoryClient {

    @GetMapping("/api/internal/inventories/search")
    InventorySearchResponseDto searchInventory(@RequestParam UUID productId);

    @PatchMapping("/api/internal/inventories/{inventoryId}")
    InventoryUpdateResponseDto updateInventory(
            @PathVariable UUID inventoryId,
            @RequestBody InventoryUpdateRequestDto request
    );
}
