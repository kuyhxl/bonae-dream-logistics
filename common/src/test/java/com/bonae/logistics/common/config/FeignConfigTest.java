package com.bonae.logistics.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeignConfig 사용자 헤더 전파")
class FeignConfigTest {

    private final RequestInterceptor interceptor = new FeignConfig().headerPropagationInterceptor();

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void givenRequestWith(String userId, String role, String hubId, String companyId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userId != null) request.addHeader("X-User-Id", userId);
        if (role != null) request.addHeader("X-User-Role", role);
        if (hubId != null) request.addHeader("X-User-Hub-Id", hubId);
        if (companyId != null) request.addHeader("X-User-Company-Id", companyId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private RequestTemplate apply() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);
        return template;
    }

    @Test
    @DisplayName("게이트웨이가 주입한 4개 헤더를 모두 전파한다")
    void propagatesAllHeaders() {
        givenRequestWith("bonaedreamida", "COMPANY_MANAGER", "hub-1", "company-1");

        RequestTemplate template = apply();

        assertThat(template.headers().get("X-User-Id")).containsExactly("bonaedreamida");
        assertThat(template.headers().get("X-User-Role")).containsExactly("COMPANY_MANAGER");
        assertThat(template.headers().get("X-User-Hub-Id")).containsExactly("hub-1");
        assertThat(template.headers().get("X-User-Company-Id")).containsExactly("company-1");
    }

    @Nested
    @DisplayName("일부 헤더만 있을 때")
    class Partial {

        @Test
        @DisplayName("소속이 없는 역할이어도 나머지는 전파한다")
        void propagatesWithoutAffiliation() {
            givenRequestWith("master01", "MASTER", null, null);

            RequestTemplate template = apply();

            assertThat(template.headers().get("X-User-Id")).containsExactly("master01");
            assertThat(template.headers().get("X-User-Role")).containsExactly("MASTER");
            assertThat(template.headers()).doesNotContainKey("X-User-Hub-Id");
            assertThat(template.headers()).doesNotContainKey("X-User-Company-Id");
        }

        @Test
        @DisplayName("role이 없어도 X-User-Id는 전파한다")
        void propagatesUserIdWithoutRole() {
            givenRequestWith("bonaedreamida", null, null, null);

            assertThat(apply().headers().get("X-User-Id")).containsExactly("bonaedreamida");
        }

        @Test
        @DisplayName("빈 문자열 헤더는 전파하지 않는다")
        void skipsBlankHeader() {
            givenRequestWith("bonaedreamida", "   ", null, null);

            RequestTemplate template = apply();

            assertThat(template.headers().get("X-User-Id")).containsExactly("bonaedreamida");
            assertThat(template.headers()).doesNotContainKey("X-User-Role");
        }
    }

    @Test
    @DisplayName("요청 컨텍스트가 없으면 아무 헤더도 추가하지 않는다")
    void noRequestContext() {
        // 스케줄러 등 웹 요청 밖에서의 호출
        RequestContextHolder.resetRequestAttributes();

        assertThat(apply().headers()).isEmpty();
    }
}
