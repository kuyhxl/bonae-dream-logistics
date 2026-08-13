package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.user.application.service.UserApprovalService;
import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.auth.RoleCheck;
import com.bonae.logistics.user.presentation.dto.request.SignupRequestSearchCondition;
import com.bonae.logistics.user.presentation.dto.request.UserApprovalRequest;
import com.bonae.logistics.user.presentation.dto.request.UserSearchCondition;
import com.bonae.logistics.user.presentation.dto.request.UserUpdateRequest;
import com.bonae.logistics.user.presentation.dto.response.SignupRequestSummaryResponse;
import com.bonae.logistics.user.presentation.dto.response.UserApprovalResponse;
import com.bonae.logistics.user.presentation.dto.response.UserDetailResponse;
import com.bonae.logistics.user.presentation.dto.response.UserSummaryResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 조회 · 수정 · 가입 승인 API")
public class UserController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    // MASTER는 소속이 없어 게이트웨이가 이 헤더를 보내지 않는다. 그래서 필수로 두지 않는다.
    private static final String USER_HUB_ID_HEADER = "X-User-Hub-Id";

    private static final String REQUESTER_HEADER_NOTE =
            "게이트웨이가 JWT를 검증한 뒤 주입한다. 클라이언트가 직접 보낸 값은 게이트웨이에서 제거된다.";

    private final UserService userService;
    private final UserApprovalService userApprovalService;

    // 사용자 목록·검색 (MASTER 전용)
    @GetMapping("/users")
    @RoleCheck(Role.MASTER)
    @Operation(summary = "사용자 목록 · 검색",
            description = "가입된 사용자를 검색 조건으로 필터링해 페이지 단위로 조회한다. 정렬은 createdAt, updatedAt만 허용하며 페이지 크기는 10, 30, 50 중 하나로 보정된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "정렬 기준이 허용 범위 밖이거나(INVALID_SORT_FIELD), 역할·상태 값이 정의되지 않았거나(INVALID_INPUT), 허브 ID 형식이 잘못됨(INVALID_UUID_FORMAT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponseDto<UserSummaryResponse>> searchUsers(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @ParameterObject @ModelAttribute UserSearchCondition condition
    ) {
        return ResponseEntity.ok(userService.searchUsers(condition, pageRequestDto));
    }

    // 사용자 단건 조회. 역할 통과 후 본인 여부는 서비스에서 다시 검사한다.
    @GetMapping("/users/{userId}")
    @RoleCheck({Role.MASTER, Role.HUB_MANAGER, Role.DELIVERY_MANAGER, Role.COMPANY_MANAGER})
    @Operation(summary = "사용자 단건 조회",
            description = "사용자 상세 정보를 조회한다. MASTER가 아니면 본인 정보만 조회할 수 있으며, 비밀번호는 어떤 경우에도 응답에 포함되지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "본인이 아닌 사용자를 조회 (FORBIDDEN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDetailResponse> getUser(
            @Parameter(description = "조회할 사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID userId,
            @Parameter(description = "요청자 아이디. " + REQUESTER_HEADER_NOTE, example = "hyerim01")
            @RequestHeader(USER_ID_HEADER) String requesterUsername,
            @Parameter(description = "요청자 역할. " + REQUESTER_HEADER_NOTE)
            @RequestHeader(USER_ROLE_HEADER) Role requesterRole
    ) {
        return ResponseEntity.ok(userService.getUser(userId, requesterUsername, requesterRole));
    }

    // 사용자 수정 (MASTER 전용)
    @PatchMapping("/users/{userId}")
    @RoleCheck(Role.MASTER)
    @Operation(summary = "사용자 수정",
            description = "사용자 정보를 부분 수정한다. 전달하지 않은 필드는 변경하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 (INVALID_INPUT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserDetailResponse> updateUser(
            @Parameter(description = "수정할 사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    // 사용자 논리 삭제 (MASTER 전용)
    @DeleteMapping("/users/{userId}")
    @RoleCheck(Role.MASTER)
    @Operation(summary = "사용자 삭제",
            description = "사용자를 논리 삭제한다. 데이터는 남고 조회 대상에서만 제외되며, 본인 계정은 삭제할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 완료"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "본인 계정은 삭제할 수 없음 (SELF_DELETE_NOT_ALLOWED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "삭제할 사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // 가입 요청 목록 조회 (MASTER, HUB_MANAGER)
    // 리터럴 경로가 /users/{userId}보다 구체적이라 Spring이 이쪽을 먼저 매칭한다.
    @GetMapping("/users/signup-requests")
    @RoleCheck({Role.MASTER, Role.HUB_MANAGER})
    @Operation(summary = "가입 요청 목록 조회",
            description = "가입 요청을 페이지 단위로 조회한다. status를 생략하면 아직 처리되지 않은 PENDING 건만 조회하며, 승인·거절 이력을 보려면 status를 명시한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "정렬 기준이 허용 범위 밖이거나(INVALID_SORT_FIELD), 상태 값이 정의되지 않음(INVALID_INPUT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponseDto<SignupRequestSummaryResponse>> searchSignupRequests(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @ParameterObject @ModelAttribute SignupRequestSearchCondition condition
    ) {
        return ResponseEntity.ok(userService.searchSignupRequests(condition, pageRequestDto));
    }

    // 가입 승인 / 거절 API. 허브 관리자의 승인 범위 제한을 위해 요청자의 역할·소속 허브를 함께 넘긴다.
    @PatchMapping("/users/{userId}/approval")
    @RoleCheck({Role.MASTER, Role.HUB_MANAGER})
    @Operation(summary = "가입 승인 · 거절",
            description = "가입 요청을 승인하거나 거절한다. 승인 시에는 역할과 소속(허브 또는 업체)을 함께 지정해야 하며, 역할과 소속이 맞지 않으면 거절된다."
                    + "\n\nHUB_MANAGER는 자신이 속한 허브의 요청만 처리할 수 있고, MASTER 권한은 누구에게도 부여할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 완료"),
            @ApiResponse(responseCode = "400", description = "처리 구분이 없거나, 역할과 소속 정보가 맞지 않음 (INVALID_INPUT, INVALID_AFFILIATION)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "MASTER 권한을 부여하려 했거나(MASTER_ROLE_NOT_ASSIGNABLE), 다른 허브의 요청을 처리하려 함(FORBIDDEN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "가입 요청을 찾을 수 없음 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 승인 또는 거절된 요청 (USER_ALREADY_PROCESSED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserApprovalResponse> processApproval(
            @Parameter(description = "처리할 가입 요청의 사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID userId,
            @Parameter(description = "요청자 역할. " + REQUESTER_HEADER_NOTE)
            @RequestHeader(USER_ROLE_HEADER) Role requesterRole,
            @Parameter(description = "요청자의 소속 허브 ID. MASTER는 소속이 없어 전달되지 않는다. " + REQUESTER_HEADER_NOTE)
            @RequestHeader(value = USER_HUB_ID_HEADER, required = false) UUID requesterHubId,
            @RequestBody @Valid UserApprovalRequest request
    ) {
        return ResponseEntity.ok(userApprovalService.process(userId, request, requesterRole, requesterHubId));
    }
}
