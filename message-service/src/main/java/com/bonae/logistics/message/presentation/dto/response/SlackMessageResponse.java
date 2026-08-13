package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "슬랙 메시지 발송 결과")
public record SlackMessageResponse(

        @Schema(description = "생성된 슬랙 메시지 ID", example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID slackMessageId,

        @Schema(description = "실제 발송된 수신자 슬랙 ID (서버가 사용자명으로 조회한 값)", example = "U08ABCDEF12")
        String receiverSlackId,

        @Schema(description = "발송 상태. 발송에 실패해도 응답은 201이므로 이 값으로 성공 여부를 판단한다.",
                example = "SUCCESS")
        SendStatus sendStatus,

        @Schema(description = "발송 재시도 횟수", example = "0")
        Integer retryCount,

        @Schema(description = "발송 완료 시각. SUCCESS가 아니면 null", example = "2026-08-13T14:32:10")
        LocalDateTime sentAt,

        @Schema(description = "메시지 생성 시각", example = "2026-08-13T14:32:09")
        LocalDateTime createdAt
) {
    public static SlackMessageResponse from(SlackMessage m) {
        return new SlackMessageResponse(
                m.getId(), m.getReceiverSlackId(), m.getSendStatus(), m.getRetryCount(), m.getSentAt(), m.getCreatedAt()
        );
    }
}
