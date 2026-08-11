package com.bonae.logistics.message.infrastructure.ai;

import lombok.Getter;

/* 재시도하면 성공할 수 있는 일시적 AI 오류 */
@Getter
public class AiTransientException extends RuntimeException {

    private final String aiErrorCode;

    public AiTransientException(String aiErrorCode) {
        super("ai transient error: " + aiErrorCode);
        this.aiErrorCode = aiErrorCode;
    }
}