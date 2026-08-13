package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "상품 상세 응답")
public class ResGetProductDto {
    @Schema(description = "상품 ID")
    private UUID productId;

    @Schema(description = "상품명")
    private String name;

    @Schema(description = "업체 ID")
    private UUID companyId;

    @Schema(description = "가격")
    private BigDecimal price;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "생성자")
    private String createdBy;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    @Schema(description = "수정자")
    private String updatedBy;

    public static ResGetProductDto from(Product product) {
        return ResGetProductDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .updatedAt(product.getUpdatedAt())
                .updatedBy(product.getUpdatedBy())
                .build();
    }
}