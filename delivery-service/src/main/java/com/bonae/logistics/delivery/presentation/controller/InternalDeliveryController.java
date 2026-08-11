package com.bonae.logistics.delivery.presentation.controller;

import com.bonae.logistics.delivery.application.service.DeliveryService;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/deliveries")
public class InternalDeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryCreateResponse> createDelivery(
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        DeliveryCreateResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<DeliveryCancelResponse> cancelDeliveryByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(deliveryService.cancelDeliveryByOrderId(orderId));
    }
}
