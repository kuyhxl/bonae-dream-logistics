package com.bonae.logistics.message.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SlackMessageRequest(
        @NotBlank(message = "수신자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String receiverSlackId,

        @NotBlank(message = "메시지는 필수입니다.")
        String message
) {
}