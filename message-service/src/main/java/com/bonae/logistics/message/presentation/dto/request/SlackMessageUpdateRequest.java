package com.bonae.logistics.message.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/* 부분 수정(PATCH). 두 필드 모두 선택값이며, 전달된 값만 반영한다. */
@Schema(description = "슬랙 메시지 수정 요청. 전달한 필드만 반영되고 생략한 필드는 유지된다.")
public record SlackMessageUpdateRequest(

        @Schema(description = "변경할 수신자 슬랙 ID. 생략하면 기존 값 유지",
                example = "U08ABCDEF12", maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "수신자 슬랙 ID는 100자를 초과할 수 없습니다.")
        String receiverSlackId,

        @Schema(description = "변경할 메시지 본문. 생략하면 기존 값 유지",
                example = "발송 시한이 16시로 변경되었습니다.", maxLength = 3000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 3000, message = "메시지는 3,000자를 초과할 수 없습니다.")
        String message
) {
}
