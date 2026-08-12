package com.bonae.logistics.order.presentation.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequestDto(

        @NotBlank(message = "상태값은 필수입니다.")
        String status

) {}