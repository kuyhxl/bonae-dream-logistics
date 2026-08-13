package com.bonae.logistics.company.config;

import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
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
                        .title("업체/상품 서비스 API")
                        .description("업체, 상품, 재고 관리")
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

    // @RoleCheck는 순수 커스텀 어노테이션이라 springdoc이 인식하지 못하므로,
    // 엔드포인트별로 필요 권한을 수동으로 적지 않아도 문서에 항상 최신 상태로 노출되도록 여기서 자동으로 덧붙인다.
    @Bean
    public OperationCustomizer roleCheckCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            RoleCheck roleCheck = handlerMethod.getMethodAnnotation(RoleCheck.class);
            if (roleCheck == null) {
                return operation;
            }

            String roles = Arrays.stream(roleCheck.value())
                    .map(UserRole::name)
                    .collect(Collectors.joining(", "));

            String description = operation.getDescription() == null ? "" : operation.getDescription();
            operation.setDescription(description + "\n\n**접근 권한:** " + roles);

            return operation;
        };
    }
}