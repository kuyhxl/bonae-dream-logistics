package com.bonae.logistics.company.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqUpdateInventoryDto {
    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotNull(message = "수량은 필수입니다.")
    private Integer quantity;

    // 0 이하/허용되지 않는 값은 INVALID_QUANTITY로, DECREASE/RESTORE가 아닌 값은 INVALID_INVENTORY_TYPE으로
    // 별도 응답해야 해서 형식(null 여부)만 여기서 걸러내고 나머지 검증은 서비스에서 수행한다.
    @NotBlank(message = "재고 변경 유형은 필수입니다.")
    private String type;
}
