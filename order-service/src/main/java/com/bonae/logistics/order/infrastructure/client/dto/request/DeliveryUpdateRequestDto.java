package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryUpdateRequestDto(
        UUID orderId,
        String requestNote   // Order의 remarks와 매핑
) {}