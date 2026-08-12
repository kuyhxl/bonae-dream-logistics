package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.request.DeliveryCancelRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.request.DeliveryCreateRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.request.DeliveryUpdateRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.DeliveryCreateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    @PostMapping("/api/internal/deliveries")
    DeliveryCreateResponseDto createDelivery(@RequestBody DeliveryCreateRequestDto request);

    @PatchMapping("/api/internal/deliveries/cancel")
    void cancelDelivery(@RequestBody DeliveryCancelRequestDto request);

    @PatchMapping("/api/internal/deliveries/update")
    void updateDelivery(@RequestBody DeliveryUpdateRequestDto request);
}

