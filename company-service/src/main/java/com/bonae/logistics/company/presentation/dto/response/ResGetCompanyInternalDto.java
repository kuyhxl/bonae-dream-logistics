package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ResGetCompanyInternalDto {
    private UUID companyId;
    private String name;
    private CompanyType type;
    private UUID hubId;
    private String address;

    // Lombok이 자동 생성하는 isDeleted() 게터는 Jackson이 "deleted"로 직렬화해버리므로
    // 게터를 직접 정의해 프로퍼티명을 "isDeleted"로 고정한다.
    @Getter(AccessLevel.NONE)
    private boolean isDeleted;

    @JsonProperty("isDeleted")
    public boolean isDeleted() {
        return isDeleted;
    }

    public static ResGetCompanyInternalDto from(Company company) {
        return ResGetCompanyInternalDto.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .address(company.getAddress())
                .isDeleted(company.isDeleted())
                .build();
    }
}