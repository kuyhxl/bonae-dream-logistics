package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageListItemResponse(
        UUID slackMessageId,
        String receiverSlackId,
        String message,
        SendStatus sendStatus,
        Integer retryCount,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static SlackMessageListItemResponse from(SlackMessage m) {
        return new SlackMessageListItemResponse(
                m.getId(), m.getReceiverSlackId(), m.getMessage(), m.getSendStatus(),
                m.getRetryCount(), m.getSentAt(), m.getCreatedAt());
    }
}