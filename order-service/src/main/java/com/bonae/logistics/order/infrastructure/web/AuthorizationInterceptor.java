package com.bonae.logistics.order.infrastructure.web;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.presentation.auth.RoleCheck;
import com.bonae.logistics.order.presentation.auth.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
        if (headerValue == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        try {
            return UserRole.valueOf(headerValue);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
