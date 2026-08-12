package com.bonae.logistics.delivery.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class InternalDeliveryUpdateRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotBlank(message = "배송 요청사항은 필수입니다.")
    @Size(max = 600, message = "배송 요청사항은 600자를 초과할 수 없습니다.")
    private String requestNote;

    public String getRequestNote() {
        return requestNote == null ? null : requestNote.trim();
    }
}
