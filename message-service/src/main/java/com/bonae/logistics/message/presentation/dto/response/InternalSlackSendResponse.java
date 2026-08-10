package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SlackMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record InternalSlackSendResponse(UUID slackMessageId, LocalDateTime sentAt) {
    public static InternalSlackSendResponse from(SlackMessage m) {
        return new InternalSlackSendResponse(m.getId(), m.getSentAt());
    }
}
