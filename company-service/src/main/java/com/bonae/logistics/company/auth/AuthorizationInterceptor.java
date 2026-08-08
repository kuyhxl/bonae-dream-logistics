package com.bonae.logistics.company.auth;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

// 게이트웨이가 JWT 검증 후 전달한 X-User-Role 헤더로 인가를 수행한다.
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RoleCheck roleCheck = handlerMethod.getMethodAnnotation(RoleCheck.class);
        if (roleCheck == null) {
            return true;
        }

        UserRole userRole = parseUserRole(request.getHeader(USER_ROLE_HEADER));
        if (!Arrays.asList(roleCheck.value()).contains(userRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return true;
    }

    private UserRole parseUserRole(String headerValue) {
        // 헤더 자체가 없으면 인증되지 않은 요청(401), 값은 있는데 알 수 없는 role이면 권한 없음(403)으로 구분한다.
        if (headerValue == null || headerValue.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UserRole.valueOf(headerValue);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}