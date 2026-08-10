package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.message.application.service.SlackMessageAdminService;
import com.bonae.logistics.message.domain.entity.UserRole;
import com.bonae.logistics.message.presentation.auth.RoleCheck;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageDetailResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/slack-messages")
@RequiredArgsConstructor
public class SlackMessageController {

    private final SlackMessageAdminService slackMessageAdminService;

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
}
