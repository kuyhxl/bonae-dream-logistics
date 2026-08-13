package com.bonae.logistics.message.presentation.dto.response;

import com.bonae.logistics.message.application.dto.AiDispatchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "AI 발송 시한 산출 결과 (서비스 간 내부 호출 전용)")
public record AiDispatchResponse(

        @Schema(description = "생성된 AI 요청/응답 로그 ID", example = "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d")
        UUID aiLogId,

        @Schema(description = "요청에 실린 주문 ID", example = "9c1b2a3d-4e5f-4a6b-8c7d-0e1f2a3b4c5d")
        UUID orderId,

        @Schema(description = "최종 발송 시한. AI 응답이 실패했거나 조건에 맞지 않으면 산술 폴백으로 계산된 값이다.",
                example = "2026-08-19T14:00:00")
        LocalDateTime finalDispatchDeadline,

        @Schema(description = "담당자에게 보낸 슬랙 메시지 ID. 발송 결과는 슬랙 메시지 조회 API로 확인한다.",
                example = "b1f2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID slackMessageId
) {
    public static AiDispatchResponse from(AiDispatchResult r) {
        return new AiDispatchResponse(r.aiLogId(), r.orderId(), r.finalDispatchDeadline(), r.slackMessageId());
    }
}
