package com.bonae.logistics.company.presentation.dto.request;

import com.bonae.logistics.company.domain.entity.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "업체 생성 요청")
public class ReqCreateCompanyDto {
    @NotBlank(message = "업체명은 필수입니다.")
    @Size(max = 100, message = "업체명은 100자 이하로 입력해주세요.")
    @Schema(description = "업체명", example = "동네반찬 공방")
    private String name;

    @NotNull(message = "업체 유형은 필수입니다.")
    @Schema(description = "업체 유형", example = "PRODUCER")
    private CompanyType type;

    @NotNull(message = "허브 ID는 필수입니다.")
    @Schema(description = "소속 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    @Schema(description = "업체 주소", example = "서울특별시 송파구 송파대로 55")
    private String address;

    // 앞뒤 공백이 저장/중복 검증에 영향을 주지 않도록 조회 시점에 trim 처리
    public String getName() {
        return name == null ? null : name.trim();
    }

    public String getAddress() {
        return address == null ? null : address.trim();
    }
}
