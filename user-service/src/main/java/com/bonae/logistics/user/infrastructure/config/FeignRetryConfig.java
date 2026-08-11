package com.bonae.logistics.user.infrastructure.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignRetryConfig {

    // 명세: Feign 호출 실패는 재시도(3회) 후 SERVICE_UNAVAILABLE(503)
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }
}