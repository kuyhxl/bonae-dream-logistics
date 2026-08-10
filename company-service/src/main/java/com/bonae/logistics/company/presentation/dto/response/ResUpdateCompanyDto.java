package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResUpdateCompanyDto {
    private UUID companyId;
    private String name;
    private CompanyType type;
    private UUID hubId;
    private String address;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static ResUpdateCompanyDto from(Company company) {
        return ResUpdateCompanyDto.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .updatedAt(company.getUpdatedAt())
                .updatedBy(company.getUpdatedBy())
                .build();
    }
}