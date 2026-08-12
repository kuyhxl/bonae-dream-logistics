package com.bonae.logistics.order.presentation.controller;

import com.bonae.logistics.order.application.service.OrderService;
import com.bonae.logistics.order.presentation.dto.internal.OrderStatusUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto request
    ) {
        orderService.updateOrderStatus(orderId, request.status());
        return ResponseEntity.ok().build();
    }
}
