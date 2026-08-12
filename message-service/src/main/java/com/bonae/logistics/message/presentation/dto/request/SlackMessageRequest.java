package com.bonae.logistics.message.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* 서비스 간 내부 호출 전용. 외부 요청은 SlackMessageSendRequest를 사용한다. */
public record SlackMessageRequest(
        @NotBlank(message = "수신자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String receiverSlackId,

        @NotBlank(message = "메시지는 필수입니다.")
        @Size(max = 3000, message = "메세지는 3000자를 초과할 수 없습니다.")
        String message
) {
}