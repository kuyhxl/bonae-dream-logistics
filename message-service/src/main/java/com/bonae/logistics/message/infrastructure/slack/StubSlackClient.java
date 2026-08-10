package com.bonae.logistics.message.infrastructure.slack;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "slack", name = "mock", havingValue = "true")
public class StubSlackClient implements SlackClient {

    @PostConstruct
    void init() {
        log.info("[SLACK] stub 모드로 동작합니다. 실제 발송은 하지 않습니다.");
    }

    @Override
    public SlackSendResult send(String receiverSlackId, String message) {
        log.info("[SLACK_STUB] to={} message={}", receiverSlackId, message);
        return SlackSendResult.ok();
    }
}
