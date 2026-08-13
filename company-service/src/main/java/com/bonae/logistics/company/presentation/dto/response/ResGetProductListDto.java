package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "상품 목록 항목 응답")
public class ResGetProductListDto {
    @Schema(description = "상품 ID")
    private UUID productId;

    @Schema(description = "상품명")
    private String name;

    @Schema(description = "업체 ID")
    private UUID companyId;

    @Schema(description = "가격")
    private BigDecimal price;

    public static ResGetProductListDto from(Product product) {
        return ResGetProductListDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .build();
    }
}
