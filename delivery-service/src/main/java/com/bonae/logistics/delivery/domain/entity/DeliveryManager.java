package com.bonae.logistics.delivery.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_delivery_managers", schema = "delivery_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "hub_id")
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_type", nullable = false, length = 20)
    private ManagerType managerType;

    @Column(name = "delivery_sequence", nullable = false)
    private Integer deliverySequence;

    private DeliveryManager(
            UUID id,
            UUID hubId,
            ManagerType managerType,
            Integer deliverySequence
    ) {
        this.id = requireNotNull(id, "배송 담당자 ID는 필수입니다.");
        this.managerType = requireNotNull(managerType, "배송 담당자 타입은 필수입니다.");
        this.deliverySequence = requireNonNegative(deliverySequence, ErrorCode.INVALID_DELIVERY_MANAGER_SEQUENCE);
        validateHubAssignment(managerType, hubId);
        this.hubId = hubId;
    }

    public static DeliveryManager create(
            UUID id,
            UUID hubId,
            ManagerType managerType,
            Integer deliverySequence
    ) {
        return new DeliveryManager(
                id,
                hubId,
                managerType,
                deliverySequence
        );
    }

    public void update(
            UUID hubId,
            ManagerType managerType,
            Integer deliverySequence
    ) {
        this.managerType = requireNotNull(managerType, "배송 담당자 타입은 필수입니다.");
        this.deliverySequence = requireNonNegative(deliverySequence, ErrorCode.INVALID_DELIVERY_MANAGER_SEQUENCE);
        validateHubAssignment(managerType, hubId);
        this.hubId = hubId;
    }

    private static <T> T requireNotNull(T value, String detail) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, detail);
        }
        return value;
    }

    private static int requireNonNegative(Integer value, ErrorCode errorCode) {
        if (value == null || value < 0) {
            throw new BusinessException(errorCode);
        }
        return value;
    }

    private static void validateHubAssignment(ManagerType managerType, UUID hubId) {
        if (managerType == ManagerType.COMPANY_DELIVERY && hubId == null) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_MANAGER_HUB_MAPPING);
        }

        if (managerType == ManagerType.HUB_DELIVERY && hubId != null) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_MANAGER_HUB_MAPPING);
        }
    }
}
