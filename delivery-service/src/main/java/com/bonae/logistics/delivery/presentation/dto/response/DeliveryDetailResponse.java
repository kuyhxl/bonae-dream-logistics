package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryDetailResponse {

    private UUID deliveryId;
    private UUID orderId;
    private UUID originHubId;
    private UUID destinationHubId;
    private UUID receiverCompanyId;
    private UUID deliveryManagerId;
    private String receiverName;
    private String receiverSlackId;
    private String deliveryAddress;
    private DeliveryStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static DeliveryDetailResponse from(Delivery delivery) {
        return DeliveryDetailResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .originHubId(delivery.getOriginHubId())
                .destinationHubId(delivery.getDestinationHubId())
                .receiverCompanyId(delivery.getReceiverCompanyId())
                .deliveryManagerId(delivery.getDeliveryManagerId())
                .receiverName(delivery.getReceiverName())
                .receiverSlackId(delivery.getReceiverSlackId())
                .deliveryAddress(delivery.getDeliveryAddress())
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .startedAt(delivery.getStartedAt())
                .completedAt(delivery.getCompletedAt())
                .createdAt(delivery.getCreatedAt())
                .createdBy(delivery.getCreatedBy())
                .updatedAt(delivery.getUpdatedAt())
                .updatedBy(delivery.getUpdatedBy())
                .build();
    }
}
