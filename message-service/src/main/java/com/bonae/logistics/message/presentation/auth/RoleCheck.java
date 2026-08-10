package com.bonae.logistics.message.presentation.auth;

import com.bonae.logistics.message.domain.entity.UserRole;

import java.lang.annotation.*;

// 접근 권한을 제한하는 커스텀 어노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleCheck {
    UserRole[] value();
}