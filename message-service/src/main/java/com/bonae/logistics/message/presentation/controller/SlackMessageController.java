package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.message.application.service.SlackMessageService;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageRequest;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/slack-messages")
@RequiredArgsConstructor
public class SlackMessageController {

    private final SlackMessageService slackMessageService;

    /* 슬랙 메시지 발송. 로그인한 모든 권한이 사용 가능 */
    @PostMapping
    public ResponseEntity<SlackMessageResponse> send(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody SlackMessageRequest request
    ){
        if (role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SlackMessageResponse.from(
                    slackMessageService.send(request.receiverSlackId(), request.message(), SourceType.USER)));
    }
}
