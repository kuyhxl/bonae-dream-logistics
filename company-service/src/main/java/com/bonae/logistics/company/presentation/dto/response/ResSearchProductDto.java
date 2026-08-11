package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ResSearchProductDto {
    private UUID productId;
    private String name;
    private UUID companyId;
    private BigDecimal price;

    public static ResSearchProductDto from(Product product) {
        return ResSearchProductDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .build();
    }
}