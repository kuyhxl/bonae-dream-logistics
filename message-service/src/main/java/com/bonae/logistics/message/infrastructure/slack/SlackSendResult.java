package com.bonae.logistics.message.infrastructure.slack;

public record SlackSendResult(boolean success, String errorCode, int attempts) {

    public static SlackSendResult ok() {
        return new SlackSendResult(true, null, 1);
    }

    public static SlackSendResult failure(String errorCode, int attempts) {
        return new SlackSendResult(false, errorCode, attempts);
    }
}
