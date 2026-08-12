package com.bonae.logistics.order.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.order.application.service.OrderService;
import com.bonae.logistics.order.presentation.auth.RoleCheck;
import com.bonae.logistics.order.presentation.auth.UserRole;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.request.OrderSearchCondition;
import com.bonae.logistics.order.presentation.dto.request.OrderUpdateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import com.bonae.logistics.order.presentation.dto.response.OrderSummaryResponseDto;
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
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId
    ) {
        orderService.cancelOrder(orderId, userId, userRole, userHubId);
        return ResponseEntity.noContent().build();
    }

    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderUpdateRequestDto request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID hubId

            ) {
        OrderResponseDto response = orderService.updateOrder(orderId, userId, request, userRole, hubId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<OrderSummaryResponseDto>> getOrders(
            OrderSearchCondition condition,
            PageRequestDto pageRequestDto,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId
    ) {
        return ResponseEntity.ok(orderService.getOrders(condition, pageRequestDto, userRole, userId, userHubId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Hub-Id", required = false) UUID userHubId
    ) {
        OrderResponseDto response = orderService.getOrder(orderId, userId, userRole, userHubId);
        return ResponseEntity.ok(response);
    }
}
