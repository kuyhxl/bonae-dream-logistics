package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageDetailResponse(
        UUID slackMessageId,
        String receiverSlackId,
        String message,
        SendStatus sendStatus,
        Integer retryCount,
        LocalDateTime sentAt,
        SourceType sourceType,
        String senderUsername,
        LocalDateTime createdAt
) {
    public static SlackMessageDetailResponse from(SlackMessage m) {
        return new SlackMessageDetailResponse(
                m.getId(), m.getReceiverSlackId(), m.getMessage(), m.getSendStatus(),
                m.getRetryCount(), m.getSentAt(), m.getSourceType(), m.getCreatedBy(), m.getCreatedAt());
    }
}