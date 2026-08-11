package com.bonae.logistics.message.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String apiUrl,
        String model,
        boolean mock, // true면 실제 호출 없이 stub 사용
        int maxAttempts, // 재시도 포함 총 시도 횟수
        Duration connectTimeout,
        Duration readTimeout
) {
}
