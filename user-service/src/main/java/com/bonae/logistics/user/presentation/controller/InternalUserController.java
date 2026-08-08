package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.presentation.dto.response.DeliveryManagerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("delivery-managers")
    public ResponseEntity<List<DeliveryManagerResponse>> getDeliveryManagers(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false)DeliveryManagerType type
    ) {
        return ResponseEntity.ok(userService.getDeliveryManagers(hubId, type));
    }
}
