package com.bonae.logistics.user.domain.entity;

public enum DeliveryManagerType {
    HUB_DELIVERY, // 허브 간 이동 담당 — 소속 허브 없음
    CUSTOMER_DELIVERY // 업체 배송 담당 — 소속 허브 있음
}
