package com.bonae.logistics.delivery.domain.delivery.entity;

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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_delivery_assignments", schema = "delivery_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAssignment extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "delivery_manager_id", nullable = false)
    private UUID deliveryManagerId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 30)
    private AssignmentStatus assignmentStatus;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "reason", length = 255)
    private String reason;

    private DeliveryAssignment(
            UUID id,
            UUID deliveryId,
            UUID deliveryManagerId,
            Integer sequenceNo,
            String reason
    ) {
        this.id = requireNotNull(id, "배송 배정 ID는 필수입니다.");
        this.deliveryId = requireNotNull(deliveryId, "배송 ID는 필수입니다.");
        this.deliveryManagerId = requireNotNull(deliveryManagerId, "배송 담당자 ID는 필수입니다.");
        this.sequenceNo = requirePositive(sequenceNo == null ? 1 : sequenceNo, ErrorCode.INVALID_DELIVERY_ASSIGNMENT_SEQUENCE);
        this.assignmentStatus = AssignmentStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
        this.reason = requireText(reason, 255, ErrorCode.INVALID_DELIVERY_ASSIGNMENT_REASON);
    }

    public static DeliveryAssignment create(
            UUID deliveryId,
            UUID deliveryManagerId,
            Integer sequenceNo,
            String reason
    ) {
        return new DeliveryAssignment(
                UUID.randomUUID(),
                deliveryId,
                deliveryManagerId,
                sequenceNo,
                reason
        );
    }

    private static <T> T requireNotNull(T value, String detail) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, detail);
        }
        return value;
    }

    private static int requirePositive(Integer value, ErrorCode errorCode) {
        if (value == null || value < 1) {
            throw new BusinessException(errorCode);
        }
        return value;
    }

    private static String requireText(String value, int maxLength, ErrorCode errorCode) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(errorCode);
        }
        return value;
    }
}
