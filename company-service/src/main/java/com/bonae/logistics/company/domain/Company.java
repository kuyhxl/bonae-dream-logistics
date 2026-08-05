package com.bonae.logistics.company.domain;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

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

    public void update(
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
}
