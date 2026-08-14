package com.bonae.logistics.delivery.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDispatchClientResponse {

    private UUID aiLogId;
    private UUID orderId;
    private LocalDateTime finalDispatchDeadline;
    private UUID slackMessageId;
}
