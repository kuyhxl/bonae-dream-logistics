package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResCreateProductDto {
    private UUID productId;
    private String name;
    private UUID companyId;
    private BigDecimal price;
    private UUID hubId;
    private Integer quantity;
    private LocalDateTime createdAt;
    private String createdBy;

    // hubId/quantity는 Product 엔티티(p_products) 컬럼이 아니라 함께 생성되는 재고 정보라 요청값을 그대로 반영
    public static ResCreateProductDto from(Product product, UUID hubId, Integer quantity) {
        return ResCreateProductDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .hubId(hubId)
                .quantity(quantity)
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .build();
    }
}