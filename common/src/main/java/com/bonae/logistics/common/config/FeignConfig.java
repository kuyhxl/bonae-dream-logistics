package com.bonae.logistics.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Configuration
public class FeignConfig {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            String userId = request.getHeader(USER_ID_HEADER);
            String userRole = request.getHeader(USER_ROLE_HEADER);

            if (userId != null && userRole != null) {
                template.header(USER_ID_HEADER, userId);
                template.header(USER_ROLE_HEADER, userRole);
            }
        };
    }
}
