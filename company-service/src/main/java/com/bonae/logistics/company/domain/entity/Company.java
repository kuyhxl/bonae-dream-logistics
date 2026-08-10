package com.bonae.logistics.company.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_companies", schema = "company_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CompanyType type;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    public Company(
            String name,
            CompanyType type,
            UUID hubId,
            String address
    ) {
        this.name = name;
        this.type = type;
        this.hubId = hubId;
        this.address = address;
    }

    // 인자로 넘어온 값이 null이면 해당 필드는 변경하지 않는다 (부분 수정)
    public void update(
            String name,
            CompanyType type,
            UUID hubId,
            String address
    ) {
        if (name != null) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
        if (hubId != null) {
            this.hubId = hubId;
        }
        if (address != null) {
            this.address = address;
        }
    }
}
