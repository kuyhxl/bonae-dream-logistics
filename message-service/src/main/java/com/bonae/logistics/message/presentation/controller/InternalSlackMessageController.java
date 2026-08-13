package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.message.application.service.SlackMessageService;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.presentation.docs.InternalErrorResponses;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageRequest;
import com.bonae.logistics.message.presentation.dto.response.InternalSlackSendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Internal Slack Message", description = """
        서비스 간 내부 호출 전용 API.

        **접근 권한** — 내부 호출 전용. 게이트웨이가 `/api/internal/**` 외부 인입을 차단하므로
        `X-User-Role` 기반 인가(401/403)를 적용하지 않는다. 외부 클라이언트는 `/api/slack-messages`를 사용한다.""")
public class InternalSlackMessageController {

    private final SlackMessageService slackMessageService;

    @PostMapping
    @Operation(
            summary = "(내부) 슬랙 메시지 발송",
            description = """
                    다른 서비스가 시스템 알림을 보낼 때 호출한다. 수신자 슬랙 ID를 직접 받으며 `sourceType`은 `SYSTEM`으로 기록된다.

                    **접근 권한** — 내부 호출 전용

                    슬랙 발송이 실패해도 메시지 기록은 남으므로 **201**로 응답한다.
                    호출 측은 알림 실패를 이유로 본래 트랜잭션을 롤백하지 않는다.""")
    @InternalErrorResponses
    @ApiResponse(responseCode = "201", description = "발송 시도 완료")
    public ResponseEntity<InternalSlackSendResponse> send(@Valid @RequestBody SlackMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InternalSlackSendResponse.from(
                        slackMessageService.send(request.receiverSlackId(), request.message(), SourceType.SYSTEM)));
    }
}
