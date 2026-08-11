package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlackMessageResponseDto(
        UUID slackMessageId,
        LocalDateTime sentAt
) {}
