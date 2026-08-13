package com.bonae.logistics.order.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.order.application.service.OrderService;
import com.bonae.logistics.order.presentation.dto.internal.OrderStatusUpdateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "[내부전용] 배송 상태에 따른 주문 상태 변경",
            description = "배송 서비스가 배송 상태 변경 시 호출하는 내부 콜백 API입니다.\n\n" +
                    "**접근 권한**: 없음 (Gateway가 외부 인입을 차단하며, Eureka를 통한 서비스 간 직접 호출만 허용됩니다)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "status 값 누락, 또는 매핑되지 않는 상태값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대상 주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 종료 상태(DELIVERED/CANCELLED)이거나 역행하는 전이 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto request
    ) {
        orderService.updateOrderStatus(orderId, request.status());
        return ResponseEntity.ok().build();
    }
}
