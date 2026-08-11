package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryRouteResponse {

    private UUID routeId;
    private UUID deliveryId;
    private Integer sequenceNo;
    private UUID fromHubId;
    private UUID toHubId;
    private UUID deliveryManagerId;
    private RouteStatus routeStatus;
    private Integer distanceMeters;
    private Integer durationSeconds;
    private BigDecimal distanceKm;
    private Integer durationMin;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;
    private LocalDateTime actualDepartedAt;
    private LocalDateTime actualArrivedAt;

    public static DeliveryRouteResponse from(DeliveryRoute deliveryRoute) {
        return DeliveryRouteResponse.builder()
                .routeId(deliveryRoute.getId())
                .deliveryId(deliveryRoute.getDeliveryId())
                .sequenceNo(deliveryRoute.getSequenceNo())
                .fromHubId(deliveryRoute.getFromHubId())
                .toHubId(deliveryRoute.getToHubId())
                .deliveryManagerId(deliveryRoute.getDeliveryManagerId())
                .routeStatus(deliveryRoute.getRouteStatus())
                .distanceMeters(deliveryRoute.getDistanceMeters())
                .durationSeconds(deliveryRoute.getDurationSeconds())
                .distanceKm(deliveryRoute.getDistanceKm())
                .durationMin(deliveryRoute.getDurationMin())
                .actualDistanceKm(deliveryRoute.getActualDistanceKm())
                .actualDurationMin(deliveryRoute.getActualDurationMin())
                .actualDepartedAt(deliveryRoute.getActualDepartedAt())
                .actualArrivedAt(deliveryRoute.getActualArrivedAt())
                .build();
    }
}
