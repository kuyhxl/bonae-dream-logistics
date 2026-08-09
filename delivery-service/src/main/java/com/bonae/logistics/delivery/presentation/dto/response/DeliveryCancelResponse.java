package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryCancelResponse {

    private UUID deliveryId;
    private DeliveryStatus status;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static DeliveryCancelResponse from(Delivery delivery) {
        return DeliveryCancelResponse.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus())
                .updatedAt(delivery.getUpdatedAt())
                .updatedBy(delivery.getUpdatedBy())
                .build();
    }
}
