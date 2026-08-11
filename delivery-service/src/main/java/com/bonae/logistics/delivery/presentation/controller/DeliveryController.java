package com.bonae.logistics.delivery.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.application.service.DeliveryService;
import com.bonae.logistics.delivery.auth.RoleCheck;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery", description = "배송 API")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @Operation(summary = "배송 생성", description = "배송을 생성합니다.")
    public ResponseEntity<DeliveryCreateResponse> createDelivery(
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        DeliveryCreateResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{deliveryId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "배송 단건 조회", description = "배송 ID로 배송 상세 정보를 조회합니다.")
    public ResponseEntity<DeliveryDetailResponse> getDelivery(
            @RequestHeader("X-User-Role") String userRoleHeader,
            @RequestHeader(value = "X-User-Id", required = false) String username,
            @RequestHeader(value = "X-User-Company-Id", required = false) UUID companyId,
            @Parameter(description = "배송 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID deliveryId
    ) {
        return ResponseEntity.ok(deliveryService.getDelivery(deliveryId, UserRole.valueOf(userRoleHeader), companyId, username));
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "배송 목록 조회", description = "배송 목록을 페이징하여 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호(1-based)", example = "1"),
            @Parameter(name = "size", description = "페이지 크기 (10, 30, 50)", example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (createdAt, updatedAt)", example = "createdAt"),
            @Parameter(name = "direction", description = "정렬 방향 (asc, desc)", example = "desc")
    })
    public ResponseEntity<PageResponseDto<DeliveryListItemResponse>> getDeliveries(
            @RequestHeader("X-User-Role") String userRoleHeader,
            @RequestHeader(value = "X-User-Id", required = false) String username,
            @RequestHeader(value = "X-User-Company-Id", required = false) UUID companyId,
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto
    ) {
        return ResponseEntity.ok(deliveryService.getDeliveries(pageRequestDto, UserRole.valueOf(userRoleHeader), companyId, username));
    }

    @PatchMapping("/{deliveryId}/cancel")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @Operation(summary = "배송 취소", description = "배송을 취소합니다.")
    public ResponseEntity<DeliveryCancelResponse> cancelDelivery(
            @RequestHeader("X-User-Role") String userRoleHeader,
            @RequestHeader(value = "X-User-Id", required = false) String username,
            @PathVariable UUID deliveryId
    ) {
        return ResponseEntity.ok(deliveryService.cancelDelivery(deliveryId, UserRole.valueOf(userRoleHeader), username));
    }
}
