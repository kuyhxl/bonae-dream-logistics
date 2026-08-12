package com.bonae.logistics.delivery.presentation.dto.request;

import com.bonae.logistics.delivery.domain.entity.ManagerType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryManagerSearchRequest {

    private UUID hubId;
    private ManagerType managerType;
    private Integer deliverySequence;
}
