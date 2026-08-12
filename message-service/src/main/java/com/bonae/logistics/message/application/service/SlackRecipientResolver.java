package com.bonae.logistics.message.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.message.infrastructure.client.UserClient;
import com.bonae.logistics.message.infrastructure.client.dto.UserInfoClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/*
 * 사용자명을 슬랙 수신자 ID로 변환한다.
 * 프론트가 넘긴 값을 그대로 슬랙에 전달하지 않기 위한 경계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackRecipientResolver {

    private final UserClient userClient;

    public String resolveSlackId(String receiverUsername) {
        // 존재하지 않는 사용자면 FeignErrorDecoder가 USER_NOT_FOUND로 변환해 던진다.
        UserInfoClientResponse user = userClient.getUserInfo(receiverUsername);

        String slackId = user.getSlackId();
        if (slackId == null || slackId.isBlank()) {
            log.warn("[SLACK] 슬랙 ID 미등록 사용자에게 발송 시도 username={}", receiverUsername);
            throw new BusinessException(ErrorCode.SLACK_ID_NOT_REGISTERED);
        }
        return slackId;
    }
}