package com.bonae.logistics.message.application.service;

/*
 * 슬랙 발송 전후의 DB 작업만 담당한다.
 * 외부 API 호출과 트랜잭션을 겹치지 않게 하기위해 별도 빈으로 분리함.
 */

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.message.domain.entity.ErrorType;
import com.bonae.logistics.message.domain.entity.MessageAiErrorLog;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.entity.SourceType;
import com.bonae.logistics.message.domain.repository.MessageAiErrorLogRepository;
import com.bonae.logistics.message.domain.repository.SlackMessageRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlackMessageStore {

    private final SlackMessageRepository slackMessageRepository;
    private final MessageAiErrorLogRepository errorLogRepository;
    private static final int ERROR_CODE_MAX_LENGTH = 50;
    private static final String UNKNOWN_ERROR_CODE = "UNKNOWN";

    @Transactional
    public SlackMessage savePending(String receiverSlackId, String message, SourceType sourceType) {
        return slackMessageRepository.save(SlackMessage.pending(receiverSlackId, message, sourceType));
    }

    @Transactional
    public SlackMessage markSuccess(UUID slackMessageId){
        SlackMessage slackMessage = findById(slackMessageId);
        slackMessage.markSuccess(LocalDateTime.now());
        return slackMessage;
    }

    @Transactional
    public SlackMessage markFailed(UUID slackMessageId, String errorCode, int attempts, String traceId){
        SlackMessage slackMessage = findById(slackMessageId);
        slackMessage.markFailed(Math.max(attempts - 1, 0)); // 다시 시도한 횟수(첫 시도를 제외)

        errorLogRepository.save(MessageAiErrorLog.builder()
                .errorType(ErrorType.SLACK_SEND)
                .sourceId(slackMessageId)
                .attemptNo(Math.max(attempts, 1)) // 처음 시도를 포함한 전체 횟수
                .errorCode(normalizeErrorCode(errorCode))
                .errorMessage("[수신자 슬랙 ID]: " + slackMessage.getReceiverSlackId() + ", [메세지 내용]: " + slackMessage.getMessage())
                .traceId(traceId)
                .build());
        return slackMessage;
    }

    private SlackMessage findById(UUID slackMessageId){
        return slackMessageRepository.findById(slackMessageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLACK_MESSAGE_NOT_FOUND));
    }

    private String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return UNKNOWN_ERROR_CODE;
        }
        return errorCode.length() > ERROR_CODE_MAX_LENGTH
                ? errorCode.substring(0, ERROR_CODE_MAX_LENGTH)
                : errorCode;
    }
}
