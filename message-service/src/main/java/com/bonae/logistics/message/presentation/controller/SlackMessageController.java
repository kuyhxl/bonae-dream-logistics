package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.message.application.service.SlackMessageAdminService;
import com.bonae.logistics.message.application.service.SlackMessageService;
import com.bonae.logistics.message.application.service.SlackRecipientResolver;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.domain.entity.UserRole;
import com.bonae.logistics.message.presentation.auth.RoleCheck;
import com.bonae.logistics.message.presentation.docs.ApiErrorExamples;
import com.bonae.logistics.message.presentation.docs.CommonErrorResponses;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSendRequest;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageUpdateRequest;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageDetailResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageListItemResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/slack-messages")
@RequiredArgsConstructor
@Tag(name = "Slack Message", description = """
        슬랙 메시지 발송 및 관리 API.

        **인증** — 게이트웨이가 JWT를 검증한 뒤 `X-User-Role` 헤더로 역할을 전달한다.
        헤더가 없으면 401, 엔드포인트가 허용하지 않는 역할이면 403이다.
        각 엔드포인트의 접근 권한은 아래 설명의 `접근 권한` 항목을 참고한다.""")
public class SlackMessageController {

    // 발송(SlackMessageService)과 관리자 조회·수정·삭제(SlackMessageAdminService)는
    // 트랜잭션 경계가 달라 서비스를 분리해 두었다. 컨트롤러에서는 둘 다 사용한다.
    private final SlackMessageService slackMessageService;
    private final SlackMessageAdminService slackMessageAdminService;
    private final SlackRecipientResolver slackRecipientResolver;

    /* 슬랙 메시지 발송. 로그인한 모든 권한이 사용 가능 */
    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(
            summary = "슬랙 메시지 발송",
            description = """
                    수신자 사용자명으로 user-service에서 슬랙 ID를 조회한 뒤 메시지를 발송한다.

                    **접근 권한** — `MASTER`, `HUB_MANAGER`, `DELIVERY_MANAGER`, `COMPANY_MANAGER` (로그인한 모든 역할)

                    슬랙 발송이 실패해도 메시지 기록은 남으므로 **201**로 응답하며,
                    이때 `sendStatus`가 `FAILED`로 내려온다. 발송 결과는 상태 코드가 아니라 `sendStatus`로 판단할 것.""")
    @CommonErrorResponses
    @ApiResponse(
            responseCode = "201",
            description = "발송 시도 완료 (sendStatus로 성공 여부 확인)")
    @ApiResponse(
            responseCode = "409",
            description = ApiErrorExamples.SLACK_ID_NOT_REGISTERED_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "SLACK_ID_NOT_REGISTERED",
                            value = ApiErrorExamples.SLACK_ID_NOT_REGISTERED_EXAMPLE)))
    @ApiResponse(
            responseCode = "502",
            description = ApiErrorExamples.UPSTREAM_ERROR_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "UPSTREAM_ERROR",
                            value = ApiErrorExamples.UPSTREAM_ERROR_EXAMPLE)))
    public ResponseEntity<SlackMessageResponse> send(@Valid @RequestBody SlackMessageSendRequest request) {
        String receiverSlackId = slackRecipientResolver.resolveSlackId(request.receiverUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SlackMessageResponse.from(
                        slackMessageService.send(receiverSlackId, request.message(), SourceType.USER)));
    }

    /* 슬랙 메시지 목록·검색. 마스터 전용 */
    @GetMapping
    @RoleCheck(UserRole.MASTER)
    @Operation(
            summary = "슬랙 메시지 목록·검색",
            description = """
                    발송 이력을 조건별로 검색하고 페이징해 조회한다. 검색 조건은 모두 선택값이며 생략하면 전체가 조회된다.

                    **접근 권한** — `MASTER` 전용

                    `size`는 10·30·50만 허용되고 그 외 값은 10으로 보정된다.
                    `sort`는 `createdAt`, `updatedAt`만 허용되며 다른 값이면 400이다.""")
    @CommonErrorResponses
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<PageResponseDto<SlackMessageListItemResponse>> search(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @ParameterObject @ModelAttribute SlackMessageSearchCondition condition
    ) {
        return ResponseEntity.ok(slackMessageAdminService.search(condition, pageRequestDto));
    }

    /* 슬랙 메시지 단건 조회. 마스터 전용 */
    @GetMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(
            summary = "슬랙 메시지 단건 조회",
            description = """
                    메시지 ID로 상세 정보를 조회한다. 논리 삭제된 메시지는 조회되지 않는다.

                    **접근 권한** — `MASTER` 전용""")
    @CommonErrorResponses
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
            responseCode = "404",
            description = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "SLACK_MESSAGE_NOT_FOUND",
                            value = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_EXAMPLE)))
    public ResponseEntity<SlackMessageDetailResponse> getDetail(
            @Parameter(description = "슬랙 메시지 ID", required = true,
                    example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
            @PathVariable UUID slackMessageId) {
        return ResponseEntity.ok(slackMessageAdminService.getDetail(slackMessageId));
    }

    /* 슬랙 메시지 수정. PENDING 상태만 가능하며 마스터 전용 */
    @PatchMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(
            summary = "슬랙 메시지 수정",
            description = """
                    수신자와 메시지 내용을 부분 수정한다. 전달한 필드만 반영되고 생략한 필드는 그대로 유지된다.

                    **접근 권한** — `MASTER` 전용

                    이미 슬랙으로 나간 메시지는 내용을 바꿔도 반영되지 않으므로
                    `sendStatus`가 `PENDING`인 메시지만 수정할 수 있다. 그 외 상태면 409다.""")
    @CommonErrorResponses
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(
            responseCode = "404",
            description = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "SLACK_MESSAGE_NOT_FOUND",
                            value = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_EXAMPLE)))
    @ApiResponse(
            responseCode = "409",
            description = ApiErrorExamples.SLACK_MESSAGE_NON_EDITABLE_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "SLACK_MESSAGE_NON_EDITABLE",
                            value = ApiErrorExamples.SLACK_MESSAGE_NON_EDITABLE_EXAMPLE)))
    public ResponseEntity<SlackMessageDetailResponse> update(
            @Parameter(description = "슬랙 메시지 ID", required = true,
                    example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
            @PathVariable UUID slackMessageId,
            @Valid @RequestBody SlackMessageUpdateRequest request
    ) {
        return ResponseEntity.ok(slackMessageAdminService.update(slackMessageId, request));
    }

    /* 슬랙 메시지 논리 삭제. 마스터 전용 */
    @DeleteMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    @Operation(
            summary = "슬랙 메시지 삭제",
            description = """
                    메시지를 논리 삭제한다. 물리 삭제하지 않고 `deleted_at`, `deleted_by`(= `X-User-Id`)만 기록한다.

                    **접근 권한** — `MASTER` 전용

                    삭제된 메시지는 이후 조회·수정·삭제에서 404로 응답한다.""")
    @CommonErrorResponses
    @ApiResponse(responseCode = "204", description = "삭제 성공 (본문 없음)")
    @ApiResponse(
            responseCode = "404",
            description = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_DESC,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            name = "SLACK_MESSAGE_NOT_FOUND",
                            value = ApiErrorExamples.SLACK_MESSAGE_NOT_FOUND_EXAMPLE)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "슬랙 메시지 ID", required = true,
                    example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
            @PathVariable UUID slackMessageId) {
        slackMessageAdminService.delete(slackMessageId);
        return ResponseEntity.noContent().build();
    }
}
