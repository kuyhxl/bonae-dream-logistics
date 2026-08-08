package com.bonae.logistics.order.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateRequestDto(

        UUID receiverCompanyId,

        @Positive(message = "수량은 0보다 커야합니다.")
        Integer quantity,

        @Future(message = "납기일자는 현재 시각 이후여야 합니다.")
        LocalDateTime dueDate,

        @Size(max = 500, message = "요청사항은 500자를 초과할 수 없습니다.")
        String remarks

) {}