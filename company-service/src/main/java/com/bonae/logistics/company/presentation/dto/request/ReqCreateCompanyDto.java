package com.bonae.logistics.company.presentation.dto.request;

import com.bonae.logistics.company.domain.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqCreateCompanyDto {
    @NotBlank(message = "업체명은 필수입니다.")
    @Size(max = 100, message = "업체명은 100자 이하로 입력해주세요.")
    private String name;

    @NotNull(message = "업체 유형은 필수입니다.")
    private CompanyType type;

    @NotNull(message = "허브 ID는 필수입니다.")
    private UUID hubId;

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    private String address;

    // 앞뒤 공백이 저장/중복 검증에 영향을 주지 않도록 조회 시점에 trim 처리
    public String getName() {
        return name == null ? null : name.trim();
    }

    public String getAddress() {
        return address == null ? null : address.trim();
    }
}
