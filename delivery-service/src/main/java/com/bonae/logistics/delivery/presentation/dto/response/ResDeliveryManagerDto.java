package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ResDeliveryManagerDto {

    private UUID deliveryManagerId;
    private UUID hubId;
    private ManagerType managerType;
    private Integer deliverySequence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResDeliveryManagerDto from(DeliveryManager deliveryManager) {
        return ResDeliveryManagerDto.builder()
                .deliveryManagerId(deliveryManager.getId())
                .hubId(deliveryManager.getHubId())
                .managerType(deliveryManager.getManagerType())
                .deliverySequence(deliveryManager.getDeliverySequence())
                .createdAt(deliveryManager.getCreatedAt())
                .updatedAt(deliveryManager.getUpdatedAt())
                .build();
    }
}
