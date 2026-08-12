package com.bonae.logistics.delivery.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class InternalDeliveryUpdateRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotNull(message = "공급 업체 ID는 필수입니다.")
    private UUID supplierCompanyId;

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    private UUID receiverCompanyId;

    @NotBlank(message = "수령인 username은 필수입니다.")
    private String receiverUsername;

    public String getReceiverUsername() {
        return receiverUsername == null ? null : receiverUsername.trim();
    }
}
