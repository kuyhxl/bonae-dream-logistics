package com.bonae.logistics.message.presentation.dto.request;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiDispatchRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        String orderNo,

        @NotBlank(message = "주문자 이름은 필수입니다.")
        String ordererName,

        @NotBlank(message = "주문자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String ordererSlackId,

        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        @NotNull(message = "납기 일시는 필수입니다.")
        LocalDateTime dueDate,

        String requestNote,

        @NotBlank(message = "발송지 허브명은 필수입니다.")
        String originHubName,

        List<String> waypointHubNames,

        @NotBlank(message = "도착지 주소는 필수입니다.")
        String destinationAddress,

        @NotNull(message = "총 예상 소요 시간은 필수입니다.")
        @PositiveOrZero(message = "총 예상 소요 시간은 0 이상이어야 합니다.")
        Integer totalDurationMin,

        @NotBlank(message = "발송 허브 담당자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String managerSlackId
) {
    public AiDispatchCommand toCommand() {
        return new AiDispatchCommand(
                orderId, orderNo, ordererName, ordererSlackId, productName, quantity,
                dueDate, requestNote, originHubName, waypointHubNames, destinationAddress, totalDurationMin, managerSlackId);
    }
}