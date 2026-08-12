package com.bonae.logistics.message.infrastructure.slack;

import com.bonae.logistics.message.infrastructure.config.SlackProperties;
import com.bonae.logistics.message.infrastructure.slack.dto.SlackPostMessageRequest;
import com.bonae.logistics.message.infrastructure.slack.dto.SlackPostMessageResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "slack", name = "mock", havingValue = "false", matchIfMissing = true)
public class SlackApiClient implements SlackClient {

    /* 재시도가 의미 있는 슬랙 에러. 나머지(channel_not_found, invalid_auth 등)는 몇 번을 보내도 같은 결과다. */
    private static final Set<String> RETRYABLE = Set.of(
            "ratelimited", "rate_limited", "internal_error",
            "service_unavailable", "fatal_error", "request_timeout"
    );

    private final RestClient restClient;
    private final int maxAttempts;

    public SlackApiClient(RestClient slackRestClient, SlackProperties properties) {
        this.maxAttempts = properties.maxAttempts();
        this.restClient = slackRestClient;
    }

    @Override
    @Retry(name = "slack", fallbackMethod = "sendFallback")
    public SlackSendResult send(String receiverSlackId, String message) {
        // DB에는 원문을 남기고, 슬랙으로 나가는 값만 이스케이프한다.
        String safeText = SlackTextEscaper.escape(message);

        SlackPostMessageResponse response = restClient.post()
                .uri("/chat.postMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SlackPostMessageRequest(receiverSlackId, safeText))
                .retrieve()
                .body(SlackPostMessageResponse.class);

        // 슬랙은 실패해도 HTTP 200을 준다. 본문의 ok로 판단해야 한다.
        if (response == null) {
            throw new SlackTransientException("empty_response");
        }
        if (!response.ok()) {
            if (RETRYABLE.contains(response.error())) {
                throw new SlackTransientException(response.error()); // 재시도 에러
            }
            log.warn("[SLACK] 재시도 불가 오류 to={} error={}", receiverSlackId, response.error());
            return SlackSendResult.failure(response.error(), 1);
        }
        return SlackSendResult.ok();
    }

    /* 재시도를 모두 소진했을 때 호출된다. 예외를 밖으로 던지지 않고 실패 결과로 변환한다. */
    private SlackSendResult sendFallback(String receiverSlackId, String message, Throwable t) {
        String errorCode = (t instanceof SlackTransientException e) ? e.getSlackErrorCode() : t.getClass().getSimpleName();
        log.error("[SLACK] 재시도 소진 to={} error={}", receiverSlackId, errorCode);
        return SlackSendResult.failure(errorCode, maxAttempts);
    }
}
