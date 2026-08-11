package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.application.dto.AiDispatchResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiDispatchResponse(
        UUID aiLogId,
        UUID orderId,
        LocalDateTime finalDispatchDeadline,
        UUID slackMessageId
) {
    public static AiDispatchResponse from(AiDispatchResult r) {
        return new AiDispatchResponse(r.aiLogId(), r.orderId(), r.finalDispatchDeadline(), r.slackMessageId());
    }
}