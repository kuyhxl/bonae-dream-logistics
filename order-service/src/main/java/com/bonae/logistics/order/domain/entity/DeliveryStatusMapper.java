package com.bonae.logistics.order.domain.entity;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;

public class DeliveryStatusMapper {

    // 인스턴스화 막음
    private DeliveryStatusMapper() {}

    public static OrderStatus toOrderStatus(String deliveryStatus) {
        return switch (deliveryStatus) {
            case "READY", "HUB_WAITING" -> OrderStatus.PENDING;
            case "HUB_MOVING", "OUT_FOR_DELIVERY" -> OrderStatus.IN_TRANSIT;
            case "DELIVERED" -> OrderStatus.DELIVERED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }
}
