package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Inventory;
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

    // hubId/quantity는 Product 엔티티(p_products) 컬럼이 아니라 함께 생성된 Inventory(p_inventories)의 값을 반영
    public static ResCreateProductDto from(Product product, Inventory inventory) {
        return ResCreateProductDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .hubId(inventory.getHubId())
                .quantity(inventory.getQuantity())
                .createdAt(product.getCreatedAt())
                .createdBy(product.getCreatedBy())
                .build();
    }
}