package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "허브명 또는 주소 중복 (HUB_NAME_DUPLICATED, HUB_ADDRESS_DUPLICATED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubDetailResponse> createHub(@Valid @RequestBody HubCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hubService.create(request));
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "허브 목록/검색", description = "허브 목록을 허브명/주소 키워드로 검색하고 정렬/페이징하여 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "허용하지 않는 정렬 기준 (INVALID_SORT_FIELD)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponseDto<HubListItemResponse>> getHubs(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto, @Parameter(description = "허브명/주소 검색어", example = "서울") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hubService.getHubs(pageRequestDto, keyword));
    }

    @GetMapping("/{hubId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "허브 단건 조회", description = "허브 ID로 단건 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "허브 없음 또는 논리 삭제됨 (HUB_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubDetailResponse> getHubDetail(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId) {
        return ResponseEntity.ok(hubService.getHubDetail(hubId));
    }

    @PatchMapping("/{hubId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "허브 수정", description = "허브 정보를 부분 수정한다. 전달된 필드만 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 수정할 필드 없음 (INVALID_INPUT)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "허브 없음 또는 논리 삭제됨 (HUB_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "허브명 또는 주소 중복 (HUB_NAME_DUPLICATED, HUB_ADDRESS_DUPLICATED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubDetailResponse> updateHub(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId, @Valid @RequestBody HubUpdateRequest request) {
        return ResponseEntity.ok(hubService.update(hubId, request));
    }

    @DeleteMapping("/{hubId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "허브 삭제", description = "허브를 논리 삭제한다. 연관된 이동정보도 함께 비활성화된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "허브 없음 또는 논리 삭제됨 (HUB_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteHub(@Parameter(description = "허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID hubId) {
        hubService.delete(hubId);
        return ResponseEntity.noContent().build();
    }

}
