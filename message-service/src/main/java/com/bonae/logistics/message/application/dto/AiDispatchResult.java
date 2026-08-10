package com.bonae.logistics.message.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiDispatchResult(
        UUID aiLogId,
        UUID orderId,
        LocalDateTime finalDispatchDeadline,
        UUID slackMessageId
) {
}