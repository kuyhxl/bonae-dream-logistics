package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.DeliveryCreateRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.DeliveryCreateResponseDto;
import com.bonae.logistics.order.infrastructure.config.OrderFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service", configuration = OrderFeignConfig.class)
public interface DeliveryClient {

    @PostMapping("/api/internal/deliveries")
    DeliveryCreateResponseDto createDelivery(@RequestBody DeliveryCreateRequestDto request);
}
