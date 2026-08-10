package com.bonae.logistics.company.presentation.dto.request;

import com.bonae.logistics.company.domain.entity.CompanyType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 부분 수정(PATCH) 요청이므로 모든 필드는 선택값이다. null이면 해당 값을 변경하지 않는다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqUpdateCompanyDto {
    @Size(max = 100, message = "업체명은 100자 이하로 입력해주세요.")
    private String name;

    private CompanyType type;

    private UUID hubId;

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