package com.bonae.logistics.company.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 부분 수정(PATCH) 요청이므로 모든 필드는 선택값이다. null이면 해당 값을 변경하지 않는다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 수정 요청 (부분 수정, 전달된 필드만 반영)")
public class ReqUpdateProductDto {
    @Size(max = 100, message = "상품명은 100자 이하로 입력해주세요.")
    @Schema(description = "상품명", example = "국내산 삼겹살 1kg")
    private String name;

    @Schema(description = "가격", example = "16000.00")
    private BigDecimal price;

    // 앞뒤 공백이 저장/중복 검증에 영향을 주지 않도록 조회 시점에 trim 처리
    public String getName() {
        return name == null ? null : name.trim();
    }
}