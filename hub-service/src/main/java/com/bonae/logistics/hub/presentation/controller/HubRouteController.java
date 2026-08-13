package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
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
@RequestMapping("/api/hub-routes")
@Tag(name = "HubRoute", description = "허브 이동정보 관리 API")
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @PostMapping
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 생성", description = "두 허브 간 직접 이동정보(간선)을 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 출발·도착 허브 동일 (SAME_HUB_ROUTE_ENDPOINTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "출발 또는 도착 허브 없음 (HUB_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "동일 방향 이동정보 중복 (HUB_ROUTE_DUPLICATED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubRouteDetailResponse> createHubRoute(@Valid @RequestBody HubRouteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hubRouteService.create(request));

    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "이동정보 목록/검색", description = "이동정보 목록을 출발/도착 허브명 키워드로 검색하고 정렬/페이징하여 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "허용하지 않는 정렬 기준 (INVALID_SORT_FIELD)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponseDto<HubRouteListItemResponse>> getHubRoutes(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto, @Parameter(description = "출발/도착 허브명 검색어", example = "서울") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hubRouteService.getHubRoutes(pageRequestDto, keyword));
    }

    @GetMapping("/{hubRouteId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "이동정보 단건 조회", description = "이동정보 ID로 단건 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "이동정보 없음 또는 논리 삭제됨 (HUB_ROUTE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubRouteDetailResponse> getHubRouteDetail(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId) {
        return ResponseEntity.ok(hubRouteService.getHubRouteDetail(hubRouteId));
    }

    @PatchMapping("/{hubRouteId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 수정", description = "이동정보의 거리/소요시간을 부분 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 수정할 필드 없음 (INVALID_INPUT)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "이동정보 없음 또는 논리 삭제됨 (HUB_ROUTE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HubRouteDetailResponse> updateHubRoute(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId, @Valid @RequestBody HubRouteUpdateRequest request) {
        return ResponseEntity.ok(hubRouteService.update(hubRouteId, request));
    }

    @DeleteMapping("/{hubRouteId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(summary = "이동정보 삭제", description = "이동정보를 논리 삭제한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "이동정보 없음 또는 논리 삭제됨 (HUB_ROUTE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteHubRoute(@Parameter(description = "이동정보 ID") @PathVariable UUID hubRouteId) {
        hubRouteService.delete(hubRouteId);
        return ResponseEntity.noContent().build();
    }
}
