package com.bonae.logistics.hub.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_hub_routes", schema = "hub_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HubRoute extends BaseEntity {

    @Id
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
            UUID id,
            Hub departureHub,
            Hub arrivalHub,
            Integer distanceMeters,
            Integer durationSeconds
    ) {
        validateHubs(departureHub, arrivalHub);
        validateDistance(distanceMeters);
        validateDuration(durationSeconds);

        this.id = id;
        this.departureHub = departureHub;
        this.arrivalHub = arrivalHub;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public static HubRoute create(
            Hub departureHub,
            Hub arrivalHub,
            Integer distanceMeters,
            Integer durationSeconds
    ) {
        return new HubRoute(
                UUID.randomUUID(),
                departureHub,
                arrivalHub,
                distanceMeters,
                durationSeconds
        );
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
        if (departureHub == null || arrivalHub == null) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 필수입니다.");
        }

        if (departureHub.getId().equals(arrivalHub.getId())) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 달라야 합니다.");
        }
    }

    private static void validateDistance(Integer distanceMeters) {
        if (distanceMeters == null || distanceMeters <= 0) {
            throw new IllegalArgumentException("이동 거리는 0보다 커야 합니다.");
        }
    }

    private static void validateDuration(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new IllegalArgumentException("소요 시간은 0보다 커야 합니다.");
        }
    }
}