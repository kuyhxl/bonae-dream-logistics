package com.bonae.logistics.delivery.presentation.dto.request;

import com.bonae.logistics.delivery.domain.delivery.entity.ManagerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqUpdateDeliveryManagerDto {

    private UUID hubId;

    @NotNull(message = "배송 담당자 타입은 필수입니다.")
    private ManagerType managerType;

    @NotNull(message = "배송 순번은 필수입니다.")
    @Min(value = 0, message = "배송 순번은 0 이상이어야 합니다.")
    private Integer deliverySequence;
}
