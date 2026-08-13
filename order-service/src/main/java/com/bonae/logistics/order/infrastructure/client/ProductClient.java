package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.response.ProductInfoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", contextId = "productClient")
public interface ProductClient {

    @GetMapping("/api/internal/products/{productId}")
    ProductInfoResponseDto getProductInfo(@PathVariable UUID productId);
}
