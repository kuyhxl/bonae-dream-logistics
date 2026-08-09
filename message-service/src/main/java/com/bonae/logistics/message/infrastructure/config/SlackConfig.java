package com.bonae.logistics.message.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class SlackConfig {

    private final SlackProperties slackProperties;

    @Bean
    public RestClient slackRestClient() {
        return RestClient.builder()
                .baseUrl(slackProperties.apiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + slackProperties.botToken())
                .build();
    }
}