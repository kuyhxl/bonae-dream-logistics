package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.presentation.dto.response.DeliveryManagerResponse;
import com.bonae.logistics.user.presentation.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;

    // 배송 담당자 목록 조회 API
    @GetMapping("/delivery-managers")
    public ResponseEntity<List<DeliveryManagerResponse>> getDeliveryManagers(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false)DeliveryManagerType type
    ) {
        return ResponseEntity.ok(userService.getDeliveryManagers(hubId, type));
    }

    // 내부 사용자 목록 조회 API
    @GetMapping("/{username}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserInfo(username));
    }
}
