package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.message.application.service.SlackMessageService;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageRequest;
import com.bonae.logistics.message.presentation.dto.response.InternalSlackSendResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* 전 서비스 -> 메세지 서비스. 게이트웨이가 외부 인입을 차단 */
@RestController
@RequestMapping("/api/internal/slack-messages")
@RequiredArgsConstructor
public class InternalSlackMessageController {

    private final SlackMessageService slackMessageService;

    @PostMapping
    public ResponseEntity<InternalSlackSendResponse> send(@Valid @RequestBody SlackMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InternalSlackSendResponse.from(
                        slackMessageService.send(request.receiverSlackId(), request.message(), SourceType.SYSTEM)));
    }
}
