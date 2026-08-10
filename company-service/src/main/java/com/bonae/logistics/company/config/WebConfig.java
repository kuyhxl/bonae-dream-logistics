package com.bonae.logistics.company.config;

import com.bonae.logistics.company.auth.AuthorizationInterceptor;
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
        // 내부 전용 API는 게이트웨이가 외부 인입을 차단하므로 인가 대상에서 제외한다.
        registry.addInterceptor(authorizationInterceptor) //접근권한 체크 인터셉터 추가
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/internal/**");
    }
}