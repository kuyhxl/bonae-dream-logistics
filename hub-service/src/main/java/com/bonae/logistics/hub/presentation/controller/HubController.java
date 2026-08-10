package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.hub.application.service.HubService;
import com.bonae.logistics.hub.domain.entity.UserRole;
import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import com.bonae.logistics.hub.presentation.dto.request.HubUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubListItemResponse;
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
    public ResponseEntity<PageResponseDto<HubListItemResponse>> getHubs(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto, @Parameter(description = "허브명/주소 검색어", example = "서울") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hubService.getHubs(pageRequestDto, keyword));
    }

    @GetMapping("/{hubId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "허브 단건 조회", description = "허브 ID로 단건 상세 정보를 조회한다.")
    public ResponseEntity<HubDetailResponse> getHubDetail(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId) {
        return ResponseEntity.ok(hubService.getHubDetail(hubId));
    }

    @PatchMapping("/{hubId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "허브 수정", description = "허브 정보를 부분 수정한다. 전달된 필드만 수정한다.")
    public ResponseEntity<HubDetailResponse> updateHub(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId, @Valid @RequestBody HubUpdateRequest request) {
        return ResponseEntity.ok(hubService.update(hubId, request));
    }

    @DeleteMapping("/{hubId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "허브 삭제", description = "허브를 논리 삭제한다. 연관된 이동정보도 함께 비활성화된다.")
    public ResponseEntity<Void> deleteHub(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId) {
        hubService.delete(hubId);
        return ResponseEntity.noContent().build();
    }

}