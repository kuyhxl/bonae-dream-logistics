package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.hub.application.service.HubService;
import com.bonae.logistics.hub.domain.entity.UserRole;
import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "허브 목록/검색", description = "허브 목록을 허브명/주소 키워드로 검색하고 정렬/페이징하여 조회한다.")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호(1-based)", example = "1"),
            @Parameter(name = "size", description = "페이지 크기 (10, 30, 50만 허용)", example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (createdAt, updatedAt만 허용)", example = "createdAt"),
            @Parameter(name = "direction", description = "정렬 방향 (asc, desc)", example = "desc")
    })
    public ResponseEntity<PageResponseDto<HubListItemResponse>> getHubs(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto, @Parameter(description = "허브명/주소 검색어", example = "서울") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hubService.getHubs(pageRequestDto, keyword));
    }

}