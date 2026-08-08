package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.hub.application.service.HubRouteService;
import com.bonae.logistics.hub.domain.entity.UserRole;
import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteCreateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteDetailResponse;
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
@RequestMapping("/api/hub-routes")
@Tag(name = "HubRoute", description = "허브 이동정보 관리 API")
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @PostMapping
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 생성", description = "두 허브 간 직접 이동정보(간선)을 등록한다.")
    public ResponseEntity<HubRouteDetailResponse> createHubRoute(@Valid @RequestBody HubRouteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hubRouteService.create(request));

    }
}
