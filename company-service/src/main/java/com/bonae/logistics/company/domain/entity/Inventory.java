package com.bonae.logistics.company.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_inventories", schema = "company_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    private static final int MIN_QUANTITY = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 상품 보관 허브 ID. hub-service 소유 리소스라 FK 없이 UUID로만 참조한다(MSA).
    // 상품을 담당하는 허브와 실제 보관 허브가 다를 수 있어 product의 허브와는 별개로 관리한다.
    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    private Inventory(
            Product product,
            UUID hubId,
            Integer quantity
    ) {
        Objects.requireNonNull(product, "상품은 null일 수 없습니다");
        Objects.requireNonNull(hubId, "허브 ID는 null일 수 없습니다");
        validateQuantity(quantity);

        this.product = product;
        this.hubId = hubId;
        this.quantity = quantity;
    }

    public static Inventory create(Product product, UUID hubId, Integer quantity) {
        return new Inventory(product, hubId, quantity);
    }

    // 재고 수량(quantity)이 올바른 범위인지 검사하는 검증 메서드. null이거나 0보다 작으면 잘못된 수량으로 처리
    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
    }
}
