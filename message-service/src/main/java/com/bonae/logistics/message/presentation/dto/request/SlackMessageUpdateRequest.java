package com.bonae.logistics.message.presentation.dto.request;

import jakarta.validation.constraints.Size;

/* 부분 수정(PATCH). 두 필드 모두 선택값이며, 전달된 값만 반영한다. */
public record SlackMessageUpdateRequest(
        @Size(max = 100, message = "수신자 슬랙 ID는 100자를 초과할 수 없습니다.")
        String receiverSlackId,

        @Size(max = 3000, message = "메시지는 3,000자를 초과할 수 없습니다.")
        String message
) {
}