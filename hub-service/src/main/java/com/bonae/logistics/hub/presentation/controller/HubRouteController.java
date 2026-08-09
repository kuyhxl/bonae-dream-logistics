package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.hub.application.service.HubRouteService;
import com.bonae.logistics.hub.domain.entity.UserRole;
import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteCreateRequest;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "이동정보 목록/검색", description = "이동정보 목록을 출발/도착 허브명 키워드로 검색하고 정렬/페이징하여 조회한다.")
    public ResponseEntity<PageResponseDto<HubRouteListItemResponse>> getHubRoutes(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto, @Parameter(description = "출발/도착 허브명 검색어", example = "서울") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hubRouteService.getHubRoutes(pageRequestDto, keyword));
    }

    @GetMapping("/{hubRouteId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "이동정보 단건 조회", description = "이동정보 ID로 단건 상세 정보를 조회한다.")
    public ResponseEntity<HubRouteDetailResponse> getHubRouteDetail(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId) {
        return ResponseEntity.ok(hubRouteService.getHubRouteDetail(hubRouteId));
    }

    @PatchMapping("/{hubRouteId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 수정", description = "이동정보의 거리/소요시간을 부분 수정한다.")
    public ResponseEntity<HubRouteDetailResponse> updateHubRoute(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId, @Valid @RequestBody HubRouteUpdateRequest request) {
        return ResponseEntity.ok(hubRouteService.update(hubRouteId, request));
    }

    @DeleteMapping("/{hubRouteId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 삭제", description = "이동정보를 논리 삭제한다.")
    public ResponseEntity<Void> deleteHubRoute(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId) {
        hubRouteService.delete(hubRouteId);
        return ResponseEntity.noContent().build();
    }
}
