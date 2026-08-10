package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageResponse(
        UUID slackMessageId,
        String receiverSlackId,
        SendStatus sendStatus,
        Integer retryCount,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static SlackMessageResponse from(SlackMessage m) {
        return new SlackMessageResponse(
                m.getId(), m.getReceiverSlackId(), m.getSendStatus(), m.getRetryCount(), m.getSentAt(), m.getCreatedAt()
        );
    }
}