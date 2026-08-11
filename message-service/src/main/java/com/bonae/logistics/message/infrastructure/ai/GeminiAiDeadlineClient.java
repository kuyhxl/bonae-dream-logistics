package com.bonae.logistics.message.infrastructure.ai;

import com.bonae.logistics.message.infrastructure.ai.dto.GeminiGenerateRequest;
import com.bonae.logistics.message.infrastructure.ai.dto.GeminiGenerateResponse;
import com.bonae.logistics.message.infrastructure.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "gemini", name = "mock", havingValue = "false", matchIfMissing = true)
public class GeminiAiDeadlineClient implements AiDeadlineClient {

    private static final String DEADLINE_FIELD = "finalDispatchDeadline";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxAttempts;

    public GeminiAiDeadlineClient(RestClient geminiRestClient,
                                  ObjectMapper objectMapper,
                                  GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.model = properties.model();
        this.maxAttempts = properties.maxAttempts();
    }

    @Override
    @Retry(name = "gemini", fallbackMethod = "generateFallback")
    public AiDeadlineResult generate(String prompt) {
        GeminiGenerateResponse response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeminiGenerateRequest.of(prompt))
                .retrieve()
                .body(GeminiGenerateResponse.class);

        if (response == null) {
            throw new AiTransientException("empty_response");
        }

        String text = response.firstText();
        if (text == null || text.isBlank()) {
            throw new AiTransientException("empty_candidate"); // 안전필터 등으로 후보가 비는 경우
        }

        // 파싱 실패는 재시도해도 같은 결과일 가능성이 크다. 재시도 없이 폴백으로 넘긴다.
        try {
            LocalDateTime deadline = LocalDateTime.parse(
                    objectMapper.readTree(text).path(DEADLINE_FIELD).asText());
            return AiDeadlineResult.ok(deadline, text);
        } catch (Exception e) {
            log.warn("[AI] 응답 파싱 실패 raw={}", text);
            return AiDeadlineResult.failure("response_parse_error", 1);
        }
    }

    /* 재시도를 모두 소진했을 때 호출된다. 예외를 밖으로 던지지 않고 실패 결과로 변환한다. */
    private AiDeadlineResult generateFallback(String prompt, Throwable t) {
        String errorCode = (t instanceof AiTransientException e)
                ? e.getAiErrorCode() : t.getClass().getSimpleName();
        log.error("[AI] 재시도 소진 error={}", errorCode);
        return AiDeadlineResult.failure(errorCode, maxAttempts);
    }
}