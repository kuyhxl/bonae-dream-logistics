package com.bonae.logistics.message.infrastructure.ai;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "gemini", name = "mock", havingValue = "true")
public class StubAiDeadlineClient implements AiDeadlineClient {

    @PostConstruct
    void init() {
        log.info("[AI] stub 모드로 동작합니다. 실제 Gemini 호출은 하지 않습니다.");
    }

    /* 개발용 더미 시한: 내일 오전 9시. 성공 경로를 그대로 태우기 위한 값이다. */
    @Override
    public AiDeadlineResult generate(String prompt) {
        LocalDateTime deadline = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        log.info("[AI_STUB] deadline={}", deadline);
        return AiDeadlineResult.ok(deadline, "{\"finalDispatchDeadline\":\"" + deadline + "\"}");
    }
}