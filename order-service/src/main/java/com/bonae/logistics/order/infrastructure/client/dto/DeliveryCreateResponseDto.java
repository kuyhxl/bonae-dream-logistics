package com.bonae.logistics.order.infrastructure.client.dto;

import java.util.UUID;

//배송 생성 응답 Dto
public record DeliveryCreateResponseDto(
        UUID deliveryId,
        String status,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer routeCount
) {
}
