package com.bonae.logistics.company.presentation;

import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.domain.CompanyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResCreateCompanyDto {
    private UUID companyId;
    private String name;
    private CompanyType type;
    private UUID hubId;
    private String address;
    private LocalDateTime createdAt;
    private String createdBy;

    public static ResCreateCompanyDto from(Company company) {
        return ResCreateCompanyDto.builder()
                .companyId(company.getCompanyId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .createdAt(company.getCreatedAt())
                .createdBy(company.getCreatedBy())
                .build();
    }
}
