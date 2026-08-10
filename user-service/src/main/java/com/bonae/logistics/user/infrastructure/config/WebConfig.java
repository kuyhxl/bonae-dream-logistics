package com.bonae.logistics.user.infrastructure.config;

import com.bonae.logistics.user.infrastructure.auth.AuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                // Swagger·actuator 등 비 API 경로까지 타지 않도록 /api/** 로 범위를 제한한다.
                .addPathPatterns("/**")
                // 서비스 간 내부 호출은 게이트웨이에서 외부 인입이 차단되므로 인가 대상에서 제외한다.
                .excludePathPatterns("/api/internal/**");
    }
}
