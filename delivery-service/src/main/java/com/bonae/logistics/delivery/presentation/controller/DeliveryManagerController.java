package com.bonae.logistics.delivery.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.application.service.DeliveryManagerService;
import com.bonae.logistics.delivery.auth.RoleCheck;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerSearchRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryManagerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-managers")
@RequiredArgsConstructor
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<DeliveryManagerResponse> createDeliveryManager(
            @Valid @RequestBody DeliveryManagerCreateRequest reqDto
    ) {
        DeliveryManagerResponse resDto = deliveryManagerService.createDeliveryManager(reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    @GetMapping("/{deliveryManagerId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<DeliveryManagerResponse> getDeliveryManager(@PathVariable UUID deliveryManagerId) {
        return ResponseEntity.ok(deliveryManagerService.getDeliveryManager(deliveryManagerId));
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<PageResponseDto<DeliveryManagerResponse>> getDeliveryManagers(
            @ModelAttribute PageRequestDto pageRequestDto
    ) {
        return ResponseEntity.ok(deliveryManagerService.getDeliveryManagers(pageRequestDto));
    }

    @GetMapping("/search")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<PageResponseDto<DeliveryManagerResponse>> searchDeliveryManagers(
            @ModelAttribute PageRequestDto pageRequestDto,
            @ModelAttribute DeliveryManagerSearchRequest searchRequest
    ) {
        return ResponseEntity.ok(deliveryManagerService.searchDeliveryManagers(pageRequestDto, searchRequest));
    }

    @PatchMapping("/{deliveryManagerId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<DeliveryManagerResponse> updateDeliveryManager(
            @PathVariable UUID deliveryManagerId,
            @Valid @RequestBody DeliveryManagerUpdateRequest reqDto
    ) {
        return ResponseEntity.ok(deliveryManagerService.updateDeliveryManager(deliveryManagerId, reqDto));
    }

    @DeleteMapping("/{deliveryManagerId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<Void> deleteDeliveryManager(@PathVariable UUID deliveryManagerId) {
        deliveryManagerService.deleteDeliveryManager(deliveryManagerId);
        return ResponseEntity.noContent().build();
    }
}
