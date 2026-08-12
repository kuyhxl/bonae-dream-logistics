package com.bonae.logistics.delivery.presentation.dto.response;

import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class DeliveryAssignmentResponse {

    private UUID assignmentId;
    private UUID deliveryId;
    private UUID deliveryManagerId;
    private Integer sequenceNo;
    private AssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime unassignedAt;
    private String reason;

    public static DeliveryAssignmentResponse from(DeliveryAssignment deliveryAssignment) {
        return DeliveryAssignmentResponse.builder()
                .assignmentId(deliveryAssignment.getId())
                .deliveryId(deliveryAssignment.getDeliveryId())
                .deliveryManagerId(deliveryAssignment.getDeliveryManagerId())
                .sequenceNo(deliveryAssignment.getSequenceNo())
                .assignmentStatus(deliveryAssignment.getAssignmentStatus())
                .assignedAt(deliveryAssignment.getAssignedAt())
                .unassignedAt(deliveryAssignment.getUnassignedAt())
                .reason(deliveryAssignment.getReason())
                .build();
    }
}
