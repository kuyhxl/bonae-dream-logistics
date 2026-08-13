package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "상품 생성 응답")
public class ResCreateProductDto {
    @Schema(description = "상품 ID")
    private UUID productId;

    @Schema(description = "상품명")
    private String name;

    @Schema(description = "업체 ID")
    private UUID companyId;

    @Schema(description = "가격")
    private BigDecimal price;

    @Schema(description = "초기 재고를 보관한 허브 ID")
    private UUID hubId;

    @Schema(description = "초기 재고 수량")
    private Integer quantity;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "생성자")
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