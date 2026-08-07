package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.hub.application.service.HubService;
import com.bonae.logistics.hub.domain.entity.UserRole;
import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hubs")
@Tag(name = "Hub", description = "허브 관리 API")
public class HubController {

    private final HubService hubService;

    @PostMapping
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "허브 생성", description = "마스터 관리자가 새로운 허브를 등록한다.")
    public ResponseEntity<HubDetailResponse> createHub(@Valid @RequestBody HubCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hubService.create(request));
    }
}
