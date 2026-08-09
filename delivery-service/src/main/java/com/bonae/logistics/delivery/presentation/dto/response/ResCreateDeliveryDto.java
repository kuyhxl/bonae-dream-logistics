package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResCreateDeliveryDto {

    private UUID deliveryId;
    private UUID orderId;
    private UUID originHubId;
    private UUID destinationHubId;
    private UUID receiverCompanyId;
    private String receiverName;
    private String receiverSlackId;
    private String deliveryAddress;
    private DeliveryStatus status;
    private LocalDateTime createdAt;
    private String createdBy;

    public static ResCreateDeliveryDto from(Delivery delivery) {
        return ResCreateDeliveryDto.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .originHubId(delivery.getOriginHubId())
                .destinationHubId(delivery.getDestinationHubId())
                .receiverCompanyId(delivery.getReceiverCompanyId())
                .receiverName(delivery.getReceiverName())
                .receiverSlackId(delivery.getReceiverSlackId())
                .deliveryAddress(delivery.getDeliveryAddress())
                .status(delivery.getStatus())
                .createdAt(delivery.getCreatedAt())
                .createdBy(delivery.getCreatedBy())
                .build();
    }
}
