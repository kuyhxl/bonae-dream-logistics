package com.bonae.logistics.message.infrastructure.ai;

import java.time.LocalDateTime;

/* 실패해도 예외를 던지지 않는다. 호출부가 폴백으로 계속 진행해야 하기 때문. */
public record AiDeadlineResult(
        boolean success,
        LocalDateTime finalDispatchDeadline,
        String responseContent,
        String errorCode,
        int attempts
) {
    public static AiDeadlineResult ok(LocalDateTime deadline, String responseContent) {
        return new AiDeadlineResult(true, deadline, responseContent, null, 1);
    }

    public static AiDeadlineResult failure(String errorCode, int attempts) {
        return new AiDeadlineResult(false, null, null, errorCode, attempts);
    }
}