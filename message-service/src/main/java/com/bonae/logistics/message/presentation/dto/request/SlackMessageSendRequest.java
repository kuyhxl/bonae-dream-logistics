package com.bonae.logistics.message.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * 외부(프론트) 발송 요청
 * 수신자를 슬랙 ID로 직접 받으면 봇 토큰 권한으로 임의 채널에 게시할 수 있으므로,
 * 시스템 사용자명만 받고 슬랙 ID는 서버가 user-service에서 조회해 채운다.
 */
public record SlackMessageSendRequest(
        @NotBlank(message = "수신자 사용자명은 필수입니다.")
        @Size(max = 50)
        String receiverUsername,

        @NotBlank(message = "메시지는 필수입니다.")
        @Size(max = 3000, message = "메시지는 3000자를 초과할 수 없습니다.")
        String message
) {
}