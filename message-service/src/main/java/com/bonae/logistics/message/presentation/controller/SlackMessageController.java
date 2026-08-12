package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.message.application.service.SlackMessageAdminService;
import com.bonae.logistics.message.application.service.SlackMessageService;
import com.bonae.logistics.message.application.service.SlackRecipientResolver;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.domain.entity.UserRole;
import com.bonae.logistics.message.presentation.auth.RoleCheck;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSendRequest;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageUpdateRequest;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageDetailResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageListItemResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageResponse;
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
public class SlackMessageController {

    // 발송(SlackMessageService)과 관리자 조회·수정·삭제(SlackMessageAdminService)는
    // 트랜잭션 경계가 달라 서비스를 분리해 두었다. 컨트롤러에서는 둘 다 사용한다.
    private final SlackMessageService slackMessageService;
    private final SlackMessageAdminService slackMessageAdminService;
    private final SlackRecipientResolver slackRecipientResolver;

    /* 슬랙 메시지 발송. 로그인한 모든 권한이 사용 가능 */
    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER, UserRole.COMPANY_MANAGER})
    public ResponseEntity<SlackMessageResponse> send(@Valid @RequestBody SlackMessageSendRequest request) {
        String receiverSlackId = slackRecipientResolver.resolveSlackId(request.receiverUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SlackMessageResponse.from(
                        slackMessageService.send(receiverSlackId, request.message(), SourceType.USER)));
    }

    /* 슬랙 메시지 목록·검색. 마스터 전용 */
    @GetMapping
    @RoleCheck(UserRole.MASTER)
    public ResponseEntity<PageResponseDto<SlackMessageListItemResponse>> search(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @ParameterObject @ModelAttribute SlackMessageSearchCondition condition
    ) {
        return ResponseEntity.ok(slackMessageAdminService.search(condition, pageRequestDto));
    }

    /* 슬랙 메시지 단건 조회. 마스터 전용 */
    @GetMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    public ResponseEntity<SlackMessageDetailResponse> getDetail(@PathVariable UUID slackMessageId) {
        return ResponseEntity.ok(slackMessageAdminService.getDetail(slackMessageId));
    }

    /* 슬랙 메시지 수정. PENDING 상태만 가능하며 마스터 전용 */
    @PatchMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    public ResponseEntity<SlackMessageDetailResponse> update(
            @PathVariable UUID slackMessageId,
            @Valid @RequestBody SlackMessageUpdateRequest request
    ) {
        return ResponseEntity.ok(slackMessageAdminService.update(slackMessageId, request));
    }

    /* 슬랙 메시지 논리 삭제. 마스터 전용 */
    @DeleteMapping("/{slackMessageId}")
    @RoleCheck(UserRole.MASTER)
    public ResponseEntity<Void> delete(@PathVariable UUID slackMessageId) {
        slackMessageAdminService.delete(slackMessageId);
        return ResponseEntity.noContent().build();
    }
}