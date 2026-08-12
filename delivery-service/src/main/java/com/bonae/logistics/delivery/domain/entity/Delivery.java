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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_deliveries", schema = "delivery_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    @Column(name = "destination_hub_id", nullable = false)
    private UUID destinationHubId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Column(name = "delivery_manager_id")
    private UUID deliveryManagerId;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_slack_id", nullable = false, length = 50)
    private String receiverSlackId;

    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress;

    @Column(name = "request_note", length = 600)
    private String requestNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private Delivery(
            UUID id,
            UUID orderId,
            UUID originHubId,
            UUID destinationHubId,
            UUID receiverCompanyId,
            String receiverName,
            String receiverSlackId,
            String deliveryAddress,
            String requestNote
    ) {
        this.id = requireNotNull(id, "배송 ID는 필수입니다.");
        this.orderId = requireNotNull(orderId, "주문 ID는 필수입니다.");
        this.originHubId = requireNotNull(originHubId, "출발 허브 ID는 필수입니다.");
        this.destinationHubId = requireNotNull(destinationHubId, "도착 허브 ID는 필수입니다.");
        this.receiverCompanyId = requireNotNull(receiverCompanyId, "수령 업체 ID는 필수입니다.");
        this.receiverName = requireText(receiverName, 50, ErrorCode.INVALID_DELIVERY_RECEIVER_NAME);
        this.receiverSlackId = requireText(receiverSlackId, 50, ErrorCode.INVALID_DELIVERY_RECEIVER_SLACK_ID);
        this.deliveryAddress = requireText(deliveryAddress, 255, ErrorCode.INVALID_DELIVERY_ADDRESS);
        this.requestNote = requireText(requestNote, 600, "배송 요청사항은 비어 있을 수 없고 600자를 초과할 수 없습니다.");
        this.status = DeliveryStatus.READY;
    }

    public static Delivery create(
            UUID orderId,
            UUID originHubId,
            UUID destinationHubId,
            UUID receiverCompanyId,
            String receiverName,
            String receiverSlackId,
            String deliveryAddress,
            String requestNote
    ) {
        return new Delivery(
                UUID.randomUUID(),
                orderId,
                originHubId,
                destinationHubId,
                receiverCompanyId,
                receiverName,
                receiverSlackId,
                deliveryAddress,
                requestNote
        );
    }

    public void cancel() {
        if (status == DeliveryStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_COMPLETED);
        }
        if (status == DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_CANCELLED);
        }
        if (status != DeliveryStatus.READY && status != DeliveryStatus.HUB_WAITING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        this.status = DeliveryStatus.CANCELLED;
    }

    public void updateRequestNote(String requestNote) {
        if (status == DeliveryStatus.DELIVERED || status == DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        this.requestNote = requireText(requestNote, 600, "배송 요청사항은 비어 있을 수 없고 600자를 초과할 수 없습니다.");
    }

    private static <T> T requireNotNull(T value, String detail) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, detail);
        }
        return value;
    }

    private static String requireText(String value, int maxLength, ErrorCode errorCode) {
        String normalizedValue = normalizeRequiredText(value);
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(errorCode);
        }
        return normalizedValue;
    }

    private static String requireText(String value, int maxLength, String detail) {
        String normalizedValue = normalizeRequiredText(value);
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, detail);
        }
        return normalizedValue;
    }

    private static String normalizeRequiredText(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return trimmedValue;
    }
}
