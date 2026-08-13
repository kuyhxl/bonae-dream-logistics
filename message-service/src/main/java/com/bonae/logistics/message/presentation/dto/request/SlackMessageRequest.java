package com.bonae.logistics.message.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* 서비스 간 내부 호출 전용. 외부 요청은 SlackMessageSendRequest를 사용한다. */
@Schema(description = "슬랙 메시지 발송 요청 (서비스 간 내부 호출 전용)")
public record SlackMessageRequest(

        @Schema(description = "수신자 슬랙 ID. 내부 호출이므로 조회 없이 그대로 사용한다.",
                example = "U08ABCDEF12", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "수신자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String receiverSlackId,

        @Schema(description = "발송할 메시지 본문", example = "[주문 생성] 주문번호 ORD-20260813-0001",
                maxLength = 3000, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "메시지는 필수입니다.")
        @Size(max = 3000, message = "메세지는 3000자를 초과할 수 없습니다.")
        String message
) {
}
