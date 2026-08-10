package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.auth.RoleCheck;
import com.bonae.logistics.user.presentation.dto.request.UserSearchCondition;
import com.bonae.logistics.user.presentation.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

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
}