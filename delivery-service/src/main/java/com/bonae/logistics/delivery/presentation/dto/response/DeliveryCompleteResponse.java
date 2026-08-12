package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryCompleteResponse {

    private UUID deliveryId;
    private DeliveryStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static DeliveryCompleteResponse from(Delivery delivery) {
        return DeliveryCompleteResponse.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus())
                .completedAt(delivery.getCompletedAt())
                .updatedAt(delivery.getUpdatedAt())
                .updatedBy(delivery.getUpdatedBy())
                .build();
    }
}
