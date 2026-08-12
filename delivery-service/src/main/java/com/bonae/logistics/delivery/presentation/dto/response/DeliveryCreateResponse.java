package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryCreateResponse {

    private UUID deliveryId;
    private DeliveryStatus status;
    private UUID departureHubId;
    private UUID arrivalHubId;
    private Integer routeCount;

    public static DeliveryCreateResponse from(Delivery delivery, int routeCount) {
        return DeliveryCreateResponse.builder()
                .deliveryId(delivery.getId())
                .status(delivery.getStatus())
                .departureHubId(delivery.getOriginHubId())
                .arrivalHubId(delivery.getDestinationHubId())
                .routeCount(routeCount)
                .build();
    }
}
