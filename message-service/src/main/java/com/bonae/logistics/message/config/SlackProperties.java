package com.bonae.logistics.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String botToken,
        String apiUrl,
        boolean mock,    // true면 실제 발송 없이 stub 사용
        int maxAttempts  // 재시도 포함 총 시도 횟수
) {
}
