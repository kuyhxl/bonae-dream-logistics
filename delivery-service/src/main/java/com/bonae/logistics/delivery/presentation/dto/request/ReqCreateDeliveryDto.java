package com.bonae.logistics.delivery.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReqCreateDeliveryDto {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotNull(message = "출발 허브 ID는 필수입니다.")
    private UUID originHubId;

    @NotNull(message = "도착 허브 ID는 필수입니다.")
    private UUID destinationHubId;

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    private UUID receiverCompanyId;

    @NotBlank(message = "수령인명은 필수입니다.")
    @Size(max = 50, message = "수령인명은 50자 이하로 입력해주세요.")
    private String receiverName;

    @NotBlank(message = "수령인 Slack ID는 필수입니다.")
    @Size(max = 50, message = "수령인 Slack ID는 50자 이하로 입력해주세요.")
    private String receiverSlackId;

    @NotBlank(message = "배송지 주소는 필수입니다.")
    @Size(max = 255, message = "배송지 주소는 255자 이하로 입력해주세요.")
    private String deliveryAddress;

    public String getReceiverName() {
        return receiverName == null ? null : receiverName.trim();
    }

    public String getReceiverSlackId() {
        return receiverSlackId == null ? null : receiverSlackId.trim();
    }

    public String getDeliveryAddress() {
        return deliveryAddress == null ? null : deliveryAddress.trim();
    }
}
