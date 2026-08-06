package com.bonae.logistics.hub.domain.entity;

// 게이트웨이가 X-User-Role 헤더로 전달하는 사용자 권한
public enum UserRole {
    MASTER,
    HUB_MANAGER,
    DELIVERY_MANAGER,
    COMPANY_MANAGER
}