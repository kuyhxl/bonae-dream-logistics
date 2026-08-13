package com.bonae.logistics.company.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상품 생성 요청")
public class ReqCreateProductDto {
    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하로 입력해주세요.")
    @Schema(description = "상품명", example = "국내산 삼겹살 1kg")
    private String name;

    @NotNull(message = "업체 ID는 필수입니다.")
    @Schema(description = "업체 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID companyId;

    @NotNull(message = "가격은 필수입니다.")
    @Schema(description = "가격", example = "15000.00")
    private BigDecimal price;

    // 상품을 보관할 허브 ID. 상품 생성과 함께 만들어질 재고(inventory)용 필드
    @NotNull(message = "허브 ID는 필수입니다.")
    @Schema(description = "초기 재고를 보관할 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    // 초기 재고 수량. 상품 생성과 함께 만들어질 재고(inventory)용 필드.
    // 0 미만/허용 범위 초과는 INVALID_QUANTITY로 별도 응답해야 해서 범위 검증은 서비스에서 수행한다.
    @NotNull(message = "수량은 필수입니다.")
    @Schema(description = "초기 재고 수량", example = "100")
    private Integer quantity;

    // 앞뒤 공백이 저장/중복 검증에 영향을 주지 않도록 조회 시점에 trim 처리
    public String getName() {
        return name == null ? null : name.trim();
    }
}