package com.bonae.logistics.hub.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_hubs", schema = "hub_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 10)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    private Hub(
            UUID id,
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Hub create(
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        return new Hub(
                UUID.randomUUID(),
                name,
                address,
                latitude,
                longitude
        );
    }

    public void update(
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}