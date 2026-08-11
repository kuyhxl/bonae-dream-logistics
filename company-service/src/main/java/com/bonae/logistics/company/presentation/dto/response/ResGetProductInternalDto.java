package com.bonae.logistics.company.presentation.dto.response;

import com.bonae.logistics.company.domain.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ResGetProductInternalDto {
    private UUID productId;
    private String name;
    private UUID companyId;
    private BigDecimal price;

    // Lombok이 자동 생성하는 isDeleted() 게터는 Jackson이 "deleted"로 직렬화해버리므로
    // 게터를 직접 정의해 프로퍼티명을 "isDeleted"로 고정한다.
    @Getter(AccessLevel.NONE)
    private boolean isDeleted;

    @JsonProperty("isDeleted")
    public boolean isDeleted() {
        return isDeleted;
    }

    public static ResGetProductInternalDto from(Product product) {
        return ResGetProductInternalDto.builder()
                .productId(product.getId())
                .name(product.getName())
                .companyId(product.getCompany().getId())
                .price(product.getPrice())
                .isDeleted(product.isDeleted())
                .build();
    }
}