package com.bonae.logistics.delivery.presentation.controller;

import com.bonae.logistics.delivery.application.service.DeliveryService;
import com.bonae.logistics.delivery.auth.RoleCheck;
import com.bonae.logistics.delivery.auth.UserRole;
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
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<DeliveryCreateResponse> createDelivery(
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        DeliveryCreateResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{deliveryId}/cancel")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<DeliveryCancelResponse> cancelDelivery(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(deliveryService.cancelDelivery(deliveryId));
    }
}
