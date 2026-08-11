package com.bonae.logistics.order.infrastructure.client.dto.request;

public record SlackMessageRequestDto(
        String receiverSlackId,
        String message
) {
}
