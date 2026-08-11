package com.bonae.logistics.order.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreateRequestDto(

        @NotNull(message = "수령업체는 필수입니다.")
        UUID receiverCompanyId,

        @NotNull(message = "상품은 필수입니다.")
        UUID productId,

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 0보다 커야 합니다.")
        Integer quantity,

        @NotNull(message = "희망일자는 필수입니다.")
        @Future(message = "희망일자는 현재 시각 이후여야 합니다.")
        LocalDateTime dueDate,

        @Size(max = 500, message = "요청사항은 500자를 초과할 수 없습니다.")
        String remarks

) {}
