package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResGetCompanyListDto {
    private UUID companyId;
    private String name;
    private CompanyType type;
    private UUID hubId;
    private String address;
    private LocalDateTime createdAt;
    private String createdBy;

    public static ResGetCompanyListDto from(Company company) {
        return ResGetCompanyListDto.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .createdAt(company.getCreatedAt())
                .createdBy(company.getCreatedBy())
                .build();
    }
}
