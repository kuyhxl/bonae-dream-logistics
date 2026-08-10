package com.bonae.logistics.message.application.service;

import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.infrastructure.slack.SlackClient;
import com.bonae.logistics.message.infrastructure.slack.SlackSendResult;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackMessageService {

    private final SlackMessageStore slackMessageStore;
    private final SlackClient slackClient;
    private final Tracer tracer;

    /*
     * PENDING 저장 -> 커밋 -> 슬랙 발송 -> 결과 반영.
     * 발송이 실패해도 저장은 유지된다(호출자는 알림 실패로 롤백하지 않는다).
     */
    public SlackMessage send(String receiverSlackId, String message, SourceType sourceType) {
        SlackMessage pending = slackMessageStore.savePending(receiverSlackId, message, sourceType);

        SlackSendResult result = slackClient.send(receiverSlackId, message);

        return result.success()
                ? slackMessageStore.markSuccess(pending.getId())
                : slackMessageStore.markFailed(pending.getId(), result.errorCode(), result.attempts(), currentTraceId());
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}
