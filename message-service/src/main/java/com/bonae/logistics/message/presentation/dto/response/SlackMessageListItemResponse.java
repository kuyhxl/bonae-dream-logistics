package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "슬랙 메시지 목록 항목")
public record SlackMessageListItemResponse(

        @Schema(description = "슬랙 메시지 ID", example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID slackMessageId,

        @Schema(description = "수신자 슬랙 ID", example = "U08ABCDEF12")
        String receiverSlackId,

        @Schema(description = "메시지 본문", example = "오늘 15시까지 발송 부탁드립니다.")
        String message,

        @Schema(description = "발송 상태", example = "SUCCESS")
        SendStatus sendStatus,

        @Schema(description = "발송 재시도 횟수", example = "0")
        Integer retryCount,

        @Schema(description = "발송 완료 시각. SUCCESS가 아니면 null", example = "2026-08-13T14:32:10")
        LocalDateTime sentAt,

        @Schema(description = "발송자 사용자명(created_by). 내부 발송이면 SYSTEM", example = "hyerim01")
        String senderUsername,

        @Schema(description = "메시지 생성 시각", example = "2026-08-13T14:32:09")
        LocalDateTime createdAt
) {
    public static SlackMessageListItemResponse from(SlackMessage m) {
        return new SlackMessageListItemResponse(
                m.getId(), m.getReceiverSlackId(), m.getMessage(), m.getSendStatus(),
                m.getRetryCount(), m.getSentAt(), m.getCreatedBy(), m.getCreatedAt());
    }
}
