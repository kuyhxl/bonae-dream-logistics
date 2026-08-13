package com.bonae.logistics.message.presentation.dto.request;

import com.bonae.logistics.message.application.command.AiDispatchCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "AI 발송 시한 산출 요청 (서비스 간 내부 호출 전용)")
public record AiDispatchRequest(

        @Schema(description = "주문 ID", example = "9c1b2a3d-4e5f-4a6b-8c7d-0e1f2a3b4c5d",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @Schema(description = "주문 번호. 슬랙 알림 문구에 표시된다.", example = "ORD-20260813-0001")
        String orderNo,

        @Schema(description = "주문자 이름", example = "진혜림", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "주문자 이름은 필수입니다.")
        String ordererName,

        @Schema(description = "주문자 슬랙 ID", example = "U08ABCDEF12", maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "주문자 슬랙 ID는 필수입니다.")
        @Size(max = 100)
        String ordererSlackId,

        @Schema(description = "상품명", example = "무선 키보드", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @Schema(description = "주문 수량. 1 이상", example = "20", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        @Schema(description = "납기 일시. AI가 이 시각을 기준으로 최종 발송 시한을 역산한다.",
                example = "2026-08-20T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "납기 일시는 필수입니다.")
        LocalDateTime dueDate,

        @Schema(description = "주문 요청 사항", example = "오전 중 도착 희망")
        String requestNote,

        @Schema(description = "발송지(출발) 허브명", example = "서울특별시 센터",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "발송지 허브명은 필수입니다.")
        String originHubName,

        @Schema(description = "경유 허브명 목록. 직송이면 비워 둔다.",
                example = "[\"대전광역시 센터\", \"광주광역시 센터\"]")
        List<String> waypointHubNames,

        @Schema(description = "최종 도착지 주소", example = "광주광역시 서구 상무대로 123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "도착지 주소는 필수입니다.")
        String destinationAddress,

        @Schema(description = "허브 구간 총 예상 소요 시간(분). AI 실패 시 산술 폴백 계산에 쓰인다.",
                example = "310", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "총 예상 소요 시간은 필수입니다.")
        @PositiveOrZero(message = "총 예상 소요 시간은 0 이상이어야 합니다.")
        Integer totalDurationMin,

        @Schema(description = "알림을 받을 발송 허브 담당자의 슬랙 ID", example = "U08MANAGER1",
                maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
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
