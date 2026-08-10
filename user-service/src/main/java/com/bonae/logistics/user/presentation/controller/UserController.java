package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.auth.RoleCheck;
import com.bonae.logistics.user.presentation.dto.request.UserSearchCondition;
import com.bonae.logistics.user.presentation.dto.request.UserUpdateRequest;
import com.bonae.logistics.user.presentation.dto.response.UserDetailResponse;
import com.bonae.logistics.user.presentation.dto.response.UserSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final UserService userService;

    // 사용자 목록·검색 (MASTER 전용)
    @GetMapping("/users")
    @RoleCheck(Role.MASTER)
    public ResponseEntity<PageResponseDto<UserSummaryResponse>> searchUsers(
            @ModelAttribute PageRequestDto pageRequestDto,
            @ModelAttribute UserSearchCondition condition
    ) {
        return ResponseEntity.ok(userService.searchUsers(condition, pageRequestDto));
    }

    // 사용자 단건 조회. 역할 통과 후 본인 여부는 서비스에서 다시 검사한다.
    @GetMapping("/users/{userId}")
    @RoleCheck({Role.MASTER, Role.HUB_MANAGER, Role.DELIVERY_MANAGER, Role.COMPANY_MANAGER})
    public ResponseEntity<UserDetailResponse> getUser(
            @PathVariable UUID userId,
            @RequestHeader(USER_ID_HEADER) String requesterUsername,
            @RequestHeader(USER_ROLE_HEADER) Role requesterRole
    ) {
        return ResponseEntity.ok(userService.getUser(userId, requesterUsername, requesterRole));
    }

    // 사용자 수정 (MASTER 전용)
    @PatchMapping("/users/{userId}")
    @RoleCheck(Role.MASTER)
    public ResponseEntity<UserDetailResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }
}