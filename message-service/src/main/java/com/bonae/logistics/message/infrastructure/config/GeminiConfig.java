package com.bonae.logistics.message.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {

    private final GeminiProperties geminiProperties;

    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(geminiProperties.connectTimeout());
        factory.setReadTimeout(geminiProperties.readTimeout());

        return RestClient.builder()
                .baseUrl(geminiProperties.apiUrl())
                .defaultHeader("x-goog-api-key", geminiProperties.apiKey())
                .requestFactory(factory)
                .build();
    }
}
