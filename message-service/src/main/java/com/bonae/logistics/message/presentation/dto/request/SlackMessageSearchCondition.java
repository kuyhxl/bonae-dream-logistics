package com.bonae.logistics.message.presentation.dto.request;

import com.bonae.logistics.message.domain.entity.SendStatus;
import com.bonae.logistics.message.domain.entity.SourceType;

/* 전부 선택값이다. null인 조건은 where 절에서 무시된다. */
public record SlackMessageSearchCondition(
        String receiverSlackId,
        SendStatus sendStatus,
        SourceType sourceType,
        String senderUsername,
        String keyword
) {
}