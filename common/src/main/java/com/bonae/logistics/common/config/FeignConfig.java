package com.bonae.logistics.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Configuration
public class FeignConfig {

    private static final List<String> PROPAGATED_HEADERS = List.of(
            "X-User-Id",
            "X-User-Role",
            "X-User-Hub-Id",
            "X-User-Company-Id"
    );

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            // 스케줄러 등 요청 컨텍스트 밖에서의 호출은 전파할 헤더가 없음
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            // 헤더 개별 처리
            for (String header : PROPAGATED_HEADERS) {
                String value = request.getHeader(header);
                if (value != null && !value.isBlank()) {
                    template.header(header, value);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper);
    }
}
