package com.bonae.logistics.hub.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    private Hub(
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        validateName(name);
        validateAddress(address);
        validateLatitude(latitude);
        validateLongitude(longitude);

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
        validateName(name);
        validateAddress(address);
        validateLatitude(latitude);
        validateLongitude(longitude);

        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_HUB_NAME);
        }
    }

    private static void validateAddress(String address) {
        if (address == null || address.isBlank() || address.length() > 255) {
            throw new BusinessException(ErrorCode.INVALID_HUB_ADDRESS);
        }
    }

    private static void validateLatitude(Double latitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new BusinessException(ErrorCode.INVALID_HUB_LATITUDE);
        }
    }

    private static void validateLongitude(Double longitude) {
        if (longitude == null || longitude > 180.0 || longitude < -180.0) {
            throw new BusinessException(ErrorCode.INVALID_HUB_LONGITUDE);
        }
    }
}