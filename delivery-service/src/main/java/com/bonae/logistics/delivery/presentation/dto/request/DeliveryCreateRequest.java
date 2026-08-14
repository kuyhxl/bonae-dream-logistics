package com.bonae.logistics.delivery.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryCreateRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotNull(message = "공급 업체 ID는 필수입니다.")
    private UUID supplierCompanyId;

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    private UUID receiverCompanyId;

    @NotBlank(message = "수령인 username은 필수입니다.")
    private String receiverUsername;

    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "납기 일시는 필수입니다.")
    private LocalDateTime dueDate;

    @NotBlank(message = "배송 요청사항은 필수입니다.")
    @Size(max = 600, message = "배송 요청사항은 600자를 초과할 수 없습니다.")
    private String requestNote;

    public String getReceiverUsername() {
        return receiverUsername == null ? null : receiverUsername.trim();
    }

    public String getProductName() {
        return productName == null ? null : productName.trim();
    }

    public String getRequestNote() {
        return requestNote == null ? null : requestNote.trim();
    }
}
