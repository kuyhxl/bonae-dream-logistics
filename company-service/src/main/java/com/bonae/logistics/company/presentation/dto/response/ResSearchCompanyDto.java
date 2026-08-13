package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "업체 검색 결과 항목 응답")
public class ResSearchCompanyDto {
    @Schema(description = "업체 ID")
    private UUID companyId;

    @Schema(description = "업체명")
    private String name;

    @Schema(description = "업체 유형")
    private CompanyType type;

    @Schema(description = "소속 허브 ID")
    private UUID hubId;

    @Schema(description = "업체 주소")
    private String address;

    public static ResSearchCompanyDto from(Company company) {
        return ResSearchCompanyDto.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .build();
    }
}