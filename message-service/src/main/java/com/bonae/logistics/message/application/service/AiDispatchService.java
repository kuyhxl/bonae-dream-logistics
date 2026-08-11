package com.bonae.logistics.message.application.service;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import com.bonae.logistics.message.application.deadline.DispatchDeadlineCalculator;
import com.bonae.logistics.message.application.deadline.DispatchNotificationFormatter;
import com.bonae.logistics.message.application.dto.AiDispatchResult;
import com.bonae.logistics.message.application.prompt.DispatchDeadlinePromptFactory;
import com.bonae.logistics.message.application.service.AiDispatchStore.SavedDispatch;
import com.bonae.logistics.message.infrastructure.ai.AiDeadlineClient;
import com.bonae.logistics.message.infrastructure.ai.AiDeadlineResult;
import com.bonae.logistics.message.infrastructure.slack.SlackClient;
import com.bonae.logistics.message.infrastructure.slack.SlackSendResult;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDispatchService {

    private final DispatchDeadlinePromptFactory promptFactory;
    private final AiDeadlineClient aiDeadlineClient;
    private final DispatchDeadlineCalculator deadlineCalculator;
    private final DispatchNotificationFormatter formatter;
    private final AiDispatchStore aiDispatchStore;
    private final SlackMessageStore slackMessageStore;
    private final SlackClient slackClient;
    private final Tracer tracer;
    private static final String INVALID_DEADLINE_ERROR_CODE = "invalid_ai_deadline";

    /*
     * AI 시한 산출 -> (실패 시 산술 폴백) -> AI 로그 + 슬랙 메시지 PENDING 저장(한 트랜잭션)
     * -> 커밋 후 슬랙 발송 -> 결과 반영.
     * AI 실패도 슬랙 실패도 예외를 전파하지 않는다(비핵심 후처리이므로 배송 생성을 막지 않는다).
     */
    public AiDispatchResult calculateAndNotify(AiDispatchCommand command) {
        String prompt = promptFactory.create(command);
        AiDeadlineResult ai = aiDeadlineClient.generate(prompt);

        LocalDateTime deadline;
        String responseContent;

        if (ai.success() && deadlineCalculator.isAcceptable(ai.finalDispatchDeadline(), command.dueDate())) {
            deadline = ai.finalDispatchDeadline();
            responseContent = ai.responseContent();
        } else {
            // 호출 실패와 "응답은 왔지만 조건 위반"을 구분해 기록한다.
            String errorCode = ai.success() ? INVALID_DEADLINE_ERROR_CODE : ai.errorCode();
            int attempts = ai.success() ? 1 : ai.attempts();

            deadline = deadlineCalculator.fallback(command.dueDate(), command.totalDurationMin());
            responseContent = "[FALLBACK] errorCode=" + errorCode
                    + ", aiResponse=" + ai.responseContent() + ", deadline=" + deadline;
            aiDispatchStore.saveAiErrorLog(command.orderId(), errorCode, attempts, prompt, currentTraceId());
            log.warn("[AI] 산술 폴백 사용 orderId={} errorCode={} deadline={}", command.orderId(), errorCode, deadline);
        }

        String message = formatter.format(command, deadline, LocalDateTime.now());

        SavedDispatch saved = aiDispatchStore.saveLogWithPendingMessage(
                command.orderId(), prompt, responseContent, deadline, command.managerSlackId(), message);

        SlackSendResult sendResult = slackClient.send(command.managerSlackId(), message);
        if (sendResult.success()) {
            slackMessageStore.markSuccess(saved.slackMessage().getId());
        } else {
            slackMessageStore.markFailed(
                    saved.slackMessage().getId(), sendResult.errorCode(), sendResult.attempts(), currentTraceId());
        }

        return new AiDispatchResult(
                saved.aiDispatchLog().getId(),
                command.orderId(),
                deadline,
                saved.slackMessage().getId());
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}