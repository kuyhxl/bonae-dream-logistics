package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SlackMessage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "슬랙 메시지 발송 결과 (서비스 간 내부 호출 전용)")
public record InternalSlackSendResponse(

        @Schema(description = "생성된 슬랙 메시지 ID", example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID slackMessageId,

        @Schema(description = "발송 완료 시각. 발송에 실패했으면 null이며, 호출 측은 이 값으로 성공 여부를 판단한다.",
                example = "2026-08-13T14:32:10")
        LocalDateTime sentAt
) {
    public static InternalSlackSendResponse from(SlackMessage m) {
        return new InternalSlackSendResponse(m.getId(), m.getSentAt());
    }
}
