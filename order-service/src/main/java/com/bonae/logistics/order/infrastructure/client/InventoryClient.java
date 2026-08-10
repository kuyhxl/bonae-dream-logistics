package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.InventoryDeductRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.InventoryDeductResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.InventoryRestoreRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.InventoryRestoreResponseDto;
import com.bonae.logistics.order.infrastructure.config.OrderFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "product-service", configuration = OrderFeignConfig.class)
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
