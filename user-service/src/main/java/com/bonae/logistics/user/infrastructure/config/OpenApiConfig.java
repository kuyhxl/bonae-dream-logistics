package com.bonae.logistics.user.infrastructure.config;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.infrastructure.auth.RoleCheck;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";
    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Bean
    public OpenAPI openAPI() {
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));

        // 에러 응답 본문을 문서에서 펼쳐 볼 수 있도록 ErrorResponse(및 중첩 FieldError) 스키마를 등록한다.
        ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("유저 서비스 API")
                        .description("""
                                회원가입 승인, 로그인, 사용자 관리

                                모든 에러 응답은 공통 형식(`code`, `message`, `traceId`, `fields`)을 따른다.
                                `code`는 서버가 정의한 에러 코드이며, 입력값 검증 실패 시에만 `fields`에 항목별 사유가 담긴다.

                                접근 권한이 있는 API는 게이트웨이가 JWT를 검증한 뒤 `X-User-*` 헤더로 신원을 전달한다.
                                각 API의 허용 권한은 설명 하단의 **접근 권한** 항목을 참고한다.
                                """)
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(components);
    }

    /**
     * 컨트롤러의 @RoleCheck를 읽어 허용 권한과 공통 에러 응답을 문서에 자동으로 반영한다.
     * 어노테이션이 단일 출처이므로 권한이 바뀌어도 문서가 따로 낡지 않는다.
     */
    @Bean
    public OperationCustomizer roleAndErrorCustomizer() {
        return (operation, handlerMethod) -> {
            RoleCheck roleCheck = handlerMethod.getMethodAnnotation(RoleCheck.class);

            if (roleCheck != null) {
                appendRoles(operation, roleCheck);
                addErrorResponse(operation, "401", "인증되지 않았거나 토큰이 유효하지 않음");
                addErrorResponse(operation, "403", "해당 작업을 수행할 권한이 없음");
            }
            addErrorResponse(operation, "500", "서버 내부 오류");

            return operation;
        };
    }

    private void appendRoles(Operation operation, RoleCheck roleCheck) {
        String roles = Arrays.stream(roleCheck.value())
                .map(Role::name)
                .collect(Collectors.joining(", "));

        String description = operation.getDescription() == null ? "" : operation.getDescription();
        operation.setDescription(description + "\n\n**접근 권한**: " + roles);
    }

    // 개별 API가 같은 상태 코드를 이미 설명했다면 그쪽을 우선한다.
    private void addErrorResponse(Operation operation, String statusCode, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(statusCode)) {
            return;
        }
        responses.addApiResponse(statusCode, errorResponse(description));
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON,
                        new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF))));
    }
}
