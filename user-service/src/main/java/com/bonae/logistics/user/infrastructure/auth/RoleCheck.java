package com.bonae.logistics.user.infrastructure.auth;

import com.bonae.logistics.user.domain.entity.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 컨트롤러 메서드에 허용 권한을 선언한다. 실제 검사는 AuthorizationInterceptor가 수행한다.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) // 실행 중에도 읽을 수 있게 유지
public @interface RoleCheck {
    Role[] value();
}