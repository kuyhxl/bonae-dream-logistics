package com.bonae.logistics.order.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Order", description = "주문 관련 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "주문 생성",
            description = "주문을 생성합니다. 재고 차감 및 배송 생성이 함께 처리됩니다.\n\n" +
                    "**접근 권한**: 모든 로그인 사용자"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없거나(PRODUCT_NOT_FOUND), " +
                    "상품의 재고 정보를 찾을 수 없음(INVENTORY_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "배송 생성 실패 후 보상 트랜잭션(재고 복원)까지 최종 실패한 경우",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "외부 서비스 호출 재시도 최종 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER, UserRole.DELIVERY_MANAGER})
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Company-Id", required = false) UUID companyId
    ) {
        OrderResponseDto response = orderService.createOrder(request, companyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "주문 취소",
            description = "PENDING 상태의 주문을 취소합니다. 배송 취소 및 재고 복원이 함께 처리됩니다.\n\n" +
                    "**접근 권한**: MASTER, HUB_MANAGER(담당 허브만)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (역할이 아니거나 담당 허브가 아님)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문이거나(ORDER_NOT_FOUND), " +
                    "재고 정보를 찾을 수 없음(INVENTORY_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "PENDING 상태가 아닌 주문 취소 시도",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "외부 서비스 호출 재시도 최종 실패 " +
                    "(재고 복원 최종 실패 시 주문은 정합성 이슈 상태로 취소 처리됨)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(
            summary = "주문 수정",
            description = "PENDING 상태 주문의 납기일/요청사항을 수정합니다. 배송 서비스에도 변경사항이 반영됩니다.\n\n" +
                    "**접근 권한**: MASTER, HUB_MANAGER(담당 허브만)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (역할이 아니거나 담당 허브가 아님)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "PENDING 상태가 아닌 주문 수정 시도",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "외부 서비스(Delivery) 호출 재시도 최종 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(
            summary = "주문 목록 조회",
            description = "주문 목록을 검색·페이징하여 조회합니다.\n\n" +
                    "**접근 권한**: 모든 로그인 사용자 (단, 조회 범위는 역할에 따라 다름)\n" +
                    "- MASTER: 전체 조회\n" +
                    "- HUB_MANAGER: 담당 허브의 주문만\n" +
                    "- 그 외: 본인이 생성한 주문만"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "sort가 createdAt/updatedAt이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER, UserRole.DELIVERY_MANAGER})
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

    @Operation(
            summary = "주문 단건 조회",
            description = "주문 하나의 상세 정보를 조회합니다.\n\n" +
                    "**접근 권한**: 모든 로그인 사용자 (단, 조회 가능 범위는 역할에 따라 다름)\n" +
                    "- MASTER: 전체 조회 가능\n" +
                    "- HUB_MANAGER: 담당 허브의 주문만 조회 가능\n" +
                    "- 그 외(업체/배송 담당자): 본인이 생성한 주문만 조회 가능"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "본인이 생성한 주문이 아니거나 담당 허브의 주문이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER, UserRole.DELIVERY_MANAGER})
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
