package com.bonae.logistics.hub.config;

import com.bonae.logistics.hub.presentation.auth.RoleCheck;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("허브 서비스 API")
                        .description("허브 및 허브 간 이동경로 관리")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // 인가에 사용하는 @RoleCheck를 그대로 읽어 문서 설명에 덧붙인다.
    @Bean
    public OperationCustomizer roleCheckCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            RoleCheck roleCheck = handlerMethod.getMethodAnnotation(RoleCheck.class);
            if (roleCheck == null) {
                return operation;
            }

            String roles = Arrays.stream(roleCheck.value())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            String description = operation.getDescription() == null ? "" : operation.getDescription();
            operation.setDescription(description + "\n\n**접근 권한**: " + roles);
            return operation;
        };
    }
}