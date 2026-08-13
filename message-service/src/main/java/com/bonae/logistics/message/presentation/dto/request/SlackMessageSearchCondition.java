package com.bonae.logistics.message.presentation.dto.request;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SourceType;
import io.swagger.v3.oas.annotations.media.Schema;

/* 전부 선택값이다. null인 조건은 where 절에서 무시된다. */
@Schema(description = "슬랙 메시지 검색 조건. 전부 선택값이며 생략한 조건은 필터에서 제외된다.")
public record SlackMessageSearchCondition(

        @Schema(description = "수신자 슬랙 ID 정확히 일치", example = "U08ABCDEF12")
        String receiverSlackId,

        @Schema(description = "발송 상태", example = "SUCCESS")
        SendStatus sendStatus,

        @Schema(description = "발송 주체. USER=사용자 직접 발송, SYSTEM=서비스 간 내부 발송", example = "USER")
        SourceType sourceType,

        @Schema(description = "발송자 사용자명(created_by)", example = "hyerim01")
        String senderUsername,

        @Schema(description = "메시지 본문 부분 일치 검색어", example = "발송")
        String keyword
) {
}
