package com.bonae.logistics.message.infrastructure.slack;

import lombok.Getter;

/* 재시도하면 성공할 수 있는 일시적 슬랙 오류 */
@Getter
public class SlackTransientException extends RuntimeException {

    private final String slackErrorCode;

    public SlackTransientException(String slackErrorCode) {
        super("slack transient error: " + slackErrorCode);
        this.slackErrorCode = slackErrorCode;
    }
}
