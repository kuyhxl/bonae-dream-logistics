package com.bonae.logistics.order.presentation.controller;

import com.bonae.logistics.order.application.service.OrderService;
import com.bonae.logistics.order.presentation.auth.RoleCheck;
import com.bonae.logistics.order.presentation.auth.UserRole;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Company-Id", required = false) UUID companyId
    ) {
        OrderResponseDto response = orderService.createOrder(request, companyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId,
            @RequestHeader(value = "X-User-Company-Id", required = false) UUID companyId
    ) {
        orderService.cancelOrder(orderId, companyId, userRole, userHubId, userId);
        return ResponseEntity.noContent().build();
    }
}
