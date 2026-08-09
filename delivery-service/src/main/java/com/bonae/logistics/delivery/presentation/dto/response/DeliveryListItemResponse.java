package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryListItemResponse {

    private UUID deliveryId;
    private UUID orderId;
    private UUID originHubId;
    private UUID destinationHubId;
    private UUID deliveryManagerId;
    private String receiverName;
    private String deliveryAddress;
    private DeliveryStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private String createdBy;

    public static DeliveryListItemResponse from(Delivery delivery) {
        return DeliveryListItemResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .originHubId(delivery.getOriginHubId())
                .destinationHubId(delivery.getDestinationHubId())
                .deliveryManagerId(delivery.getDeliveryManagerId())
                .receiverName(delivery.getReceiverName())
                .deliveryAddress(delivery.getDeliveryAddress())
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .completedAt(delivery.getCompletedAt())
                .createdAt(delivery.getCreatedAt())
                .createdBy(delivery.getCreatedBy())
                .build();
    }
}
