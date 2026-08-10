package com.bonae.logistics.message.infrastructure.slack;

/*
 * 슬랙 발송 창구 (이후 RabbitMQ 이벤트 발행으로 교체될 지점)
 * (호출부는 이 인터페이스 외에 슬랙을 직접 알지 않는다.)
 */
public interface SlackClient {
    SlackSendResult send(String receiverSlackId, String message);
}
