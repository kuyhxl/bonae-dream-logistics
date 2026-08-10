package com.bonae.logistics.hub.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_hub_routes", schema = "hub_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HubRoute extends BaseEntity {

    // 지구 반지름(m), Haversine 계산용
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    // 평균속도 60km/h를 m/s로 환산 (시드 데이터와 동일한 가정)
    private static final double AVERAGE_SPEED_METERS_PER_SECOND = 60000.0 / 3600.0;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_hub_id", nullable = false)
    private Hub departureHub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrival_hub_id", nullable = false)
    private Hub arrivalHub;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    private HubRoute(
            Hub departureHub,
            Hub arrivalHub,
            Integer distanceMeters,
            Integer durationSeconds
    ) {
        validateHubs(departureHub, arrivalHub);
        validateDistance(distanceMeters);
        validateDuration(durationSeconds);

        this.departureHub = departureHub;
        this.arrivalHub = arrivalHub;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    // 좌표 기반으로 거리/소요시간을 자동 계산해서 생성
    public static HubRoute create(Hub departureHub, Hub arrivalHub) {
        int distanceMeters = calculateDistanceMeters(departureHub, arrivalHub);
        int durationSeconds = calculateDurationSeconds(distanceMeters);
        return new HubRoute(departureHub, arrivalHub, distanceMeters, durationSeconds);
    }


    public void update(
            Integer distanceMeters,
            Integer durationSeconds
    ) {
        validateDistance(distanceMeters);
        validateDuration(durationSeconds);

        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    private static void validateHubs(Hub departureHub, Hub arrivalHub) {
        Objects.requireNonNull(departureHub, "출발 허브는 null일 수 없습니다");
        Objects.requireNonNull(arrivalHub, "도착 허브는 null일 수 없습니다");
        if (departureHub.getId().equals(arrivalHub.getId())) {
            throw new BusinessException(ErrorCode.SAME_HUB_ROUTE_ENDPOINTS);
        }
    }

    private static void validateDistance(Integer distanceMeters) {
        if (distanceMeters == null || distanceMeters <= 0) {
            throw new BusinessException(ErrorCode.INVALID_HUB_ROUTE_DISTANCE);
        }
    }

    private static void validateDuration(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new BusinessException(ErrorCode.INVALID_HUB_ROUTE_DURATION);
        }
    }

    // Haversine 공식(구면 삼각법 기반 두 좌표 간 최단거리 계산)으로 허브 좌표 기반 직선거리(m)를 구한다. 실제 도로 거리 아님.
    private static int calculateDistanceMeters(Hub departureHub, Hub arrivalHub) {
        double lat1 = Math.toRadians(departureHub.getLatitude());
        double lat2 = Math.toRadians(arrivalHub.getLatitude());
        double deltaLat = Math.toRadians(arrivalHub.getLatitude() - departureHub.getLatitude());
        double deltaLon = Math.toRadians(arrivalHub.getLongitude() - departureHub.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }

    private static int calculateDurationSeconds(int distanceMeters) {
        return (int) Math.ceil(distanceMeters / AVERAGE_SPEED_METERS_PER_SECOND);
    }
}