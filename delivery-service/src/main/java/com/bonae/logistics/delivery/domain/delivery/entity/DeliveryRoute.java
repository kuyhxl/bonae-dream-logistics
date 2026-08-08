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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_delivery_routes", schema = "delivery_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRoute extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "from_hub_id", nullable = false)
    private UUID fromHubId;

    @Column(name = "to_hub_id", nullable = false)
    private UUID toHubId;

    @Column(name = "delivery_manager_id")
    private UUID deliveryManagerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_status", nullable = false, length = 30)
    private RouteStatus routeStatus;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "actual_distance_km", precision = 10, scale = 2)
    private BigDecimal actualDistanceKm;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Column(name = "actual_departed_at")
    private LocalDateTime actualDepartedAt;

    @Column(name = "actual_arrived_at")
    private LocalDateTime actualArrivedAt;

    private DeliveryRoute(
            UUID id,
            UUID deliveryId,
            Integer sequenceNo,
            UUID fromHubId,
            UUID toHubId,
            UUID deliveryManagerId,
            Integer distanceMeters,
            Integer durationSeconds,
            BigDecimal distanceKm,
            Integer durationMin
    ) {
        this.id = requireNotNull(id, "배송 경로 ID는 필수입니다.");
        this.deliveryId = requireNotNull(deliveryId, "배송 ID는 필수입니다.");
        this.sequenceNo = requirePositive(sequenceNo, ErrorCode.INVALID_DELIVERY_ROUTE_SEQUENCE);
        this.fromHubId = requireNotNull(fromHubId, "출발 허브 ID는 필수입니다.");
        this.toHubId = requireNotNull(toHubId, "도착 허브 ID는 필수입니다.");
        this.deliveryManagerId = deliveryManagerId;
        this.routeStatus = RouteStatus.WAITING;
        this.distanceMeters = requireNonNegative(distanceMeters, ErrorCode.INVALID_DELIVERY_ROUTE_DISTANCE);
        this.durationSeconds = requireNonNegative(durationSeconds, ErrorCode.INVALID_DELIVERY_ROUTE_DURATION);
        this.distanceKm = requireDecimal(distanceKm, 10, 2, ErrorCode.INVALID_DELIVERY_ROUTE_DISTANCE);
        this.durationMin = requireNonNegative(durationMin, ErrorCode.INVALID_DELIVERY_ROUTE_DURATION);
    }

    public static DeliveryRoute create(
            UUID deliveryId,
            Integer sequenceNo,
            UUID fromHubId,
            UUID toHubId,
            UUID deliveryManagerId,
            Integer distanceMeters,
            Integer durationSeconds,
            BigDecimal distanceKm,
            Integer durationMin
    ) {
        return new DeliveryRoute(
                UUID.randomUUID(),
                deliveryId,
                sequenceNo,
                fromHubId,
                toHubId,
                deliveryManagerId,
                distanceMeters,
                durationSeconds,
                distanceKm,
                durationMin
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

    private static Integer requireNonNegative(Integer value, ErrorCode errorCode) {
        if (value != null && value < 0) {
            throw new BusinessException(errorCode);
        }
        return value;
    }

    private static BigDecimal requireDecimal(BigDecimal value, int precision, int scale, ErrorCode errorCode) {
        if (value != null && (value.signum() < 0 || value.precision() > precision || value.scale() > scale)) {
            throw new BusinessException(errorCode);
        }
        return value;
    }
}
