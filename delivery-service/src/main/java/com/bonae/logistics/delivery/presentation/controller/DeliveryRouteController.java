package com.bonae.logistics.delivery.presentation.controller;

import com.bonae.logistics.delivery.application.service.DeliveryRouteService;
import com.bonae.logistics.delivery.auth.RoleCheck;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryRouteStatusUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryRouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Delivery Route", description = "배송 경로 API")
public class DeliveryRouteController {

    private final DeliveryRouteService deliveryRouteService;

    @GetMapping("/deliveries/{deliveryId}/routes")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "배송 경로 조회", description = "배송 ID로 경로 목록을 순서대로 조회합니다.")
    public ResponseEntity<List<DeliveryRouteResponse>> getDeliveryRoutes(
            @Parameter(description = "배송 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @RequestHeader(value = "X-User-Id", required = false) String username
    ) {
        return ResponseEntity.ok(
                deliveryRouteService.getDeliveryRoutes(deliveryId, UserRole.valueOf(userRoleHeader), username)
        );
    }

    @PatchMapping("/delivery-routes/{routeId}/status")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "배송 경로 상태 변경", description = "배송 경로 상태를 IN_TRANSIT 또는 ARRIVED로 변경합니다.")
    public ResponseEntity<DeliveryRouteResponse> updateRouteStatus(
            @Parameter(description = "배송 경로 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID routeId,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @RequestHeader(value = "X-User-Id", required = false) String username,
            @Valid @RequestBody DeliveryRouteStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                deliveryRouteService.updateRouteStatus(
                        routeId,
                        request.getRouteStatus(),
                        UserRole.valueOf(userRoleHeader),
                        username
                )
        );
    }
}
