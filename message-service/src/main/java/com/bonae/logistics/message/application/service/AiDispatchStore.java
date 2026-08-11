package com.bonae.logistics.message.application.service;

/*
 * AI 발송시한 로그와 슬랙 메시지의 DB 작업만 담당한다.
 * 외부 API 호출(Gemini/Slack)과 트랜잭션이 겹치지 않게 SlackMessageStore처럼 별도 빈으로 분리했다.
 */

import com.bonae.logistics.message.domain.entity.*;
import com.bonae.logistics.message.domain.repository.AiDispatchLogRepository;
import com.bonae.logistics.message.domain.repository.MessageAiErrorLogRepository;
import com.bonae.logistics.message.domain.repository.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiDispatchStore {

    private static final int ERROR_CODE_MAX_LENGTH = 50;
    private static final String UNKNOWN_ERROR_CODE = "UNKNOWN";

    private final SlackMessageRepository slackMessageRepository;
    private final AiDispatchLogRepository aiDispatchLogRepository;
    private final MessageAiErrorLogRepository errorLogRepository;

    /* 슬랙 메시지를 PENDING으로 먼저 만들고 그 id를 AI 로그에 채운다. 둘은 같은 트랜잭션이다. */
    @Transactional
    public SavedDispatch saveLogWithPendingMessage(UUID orderId,
                                                   String requestContent,
                                                   String responseContent,
                                                   LocalDateTime finalDispatchDeadline,
                                                   String receiverSlackId,
                                                   String message) {
        SlackMessage slackMessage = slackMessageRepository.save(
                SlackMessage.pending(receiverSlackId, message, SourceType.SYSTEM));

        AiDispatchLog aiDispatchLog = aiDispatchLogRepository.save(AiDispatchLog.builder()
                .orderId(orderId)
                .requestContent(requestContent)
                .responseContent(responseContent)
                .finalDispatchDeadline(finalDispatchDeadline)
                .slackMessageId(slackMessage.getId())
                .build());

        return new SavedDispatch(aiDispatchLog, slackMessage);
    }

    /* AI 실패는 폴백으로 정상 진행되므로, 본 트랜잭션과 무관하게 반드시 남아야 한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAiErrorLog(UUID orderId, String errorCode, int attempts, String requestContent, String traceId) {
        errorLogRepository.save(MessageAiErrorLog.builder()
                .errorType(ErrorType.AI_GENERATION)
                .sourceId(orderId)
                .attemptNo(Math.max(attempts, 1))
                .errorCode(normalizeErrorCode(errorCode))
                .errorMessage("[AI 발송시한 산출 실패] 산술 폴백값 사용. [요청 프롬프트]: " + requestContent)
                .traceId(traceId)
                .build());
    }

    private String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return UNKNOWN_ERROR_CODE;
        }
        return errorCode.length() > ERROR_CODE_MAX_LENGTH
                ? errorCode.substring(0, ERROR_CODE_MAX_LENGTH)
                : errorCode;
    }

    public record SavedDispatch(AiDispatchLog aiDispatchLog, SlackMessage slackMessage) {
    }
}