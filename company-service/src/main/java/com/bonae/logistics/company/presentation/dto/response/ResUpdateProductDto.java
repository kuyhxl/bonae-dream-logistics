package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResUpdateProductDto {
    private UUID productId;
    private String name;
    private UUID companyId;
    private BigDecimal price;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static ResUpdateProductDto from(Product product) {
        return ResUpdateProductDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .updatedAt(product.getUpdatedAt())
                .updatedBy(product.getUpdatedBy())
                .build();
    }
}