package com.bonae.logistics.order.presentation.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequestDto(

        @NotBlank(message = "상태값은 필수입니다.")
        String status   // Delivery의 상태값 원문 (예: HUB_MOVING) — Service에서 매핑 처리

) {}