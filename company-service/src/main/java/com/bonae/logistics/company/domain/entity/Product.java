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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_products", schema = "company_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    // price 컬럼 정의(decimal(12,2))가 허용하는 최댓값과 소수 자릿수
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
    private static final int MAX_PRICE_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    private Product(
            String name,
            Company company,
            BigDecimal price
    ) {
        Objects.requireNonNull(company, "업체는 null일 수 없습니다");
        validatePrice(price);

        this.name = name;
        this.company = company;
        this.price = price;
    }

    public static Product create(String name, Company company, BigDecimal price) {
        return new Product(name, company, price);
    }

    // 인자로 넘어온 값이 null이면 해당 필드는 변경하지 않는다 (부분 수정)
    public void update(String name, BigDecimal price) {
        if (name != null) {
            this.name = name;
        }
        if (price != null) {
            validatePrice(price);
            this.price = price;
        }
    }

    //상품 가격(price)이 올바른 범위인지 검사하는 검증 메서드
    // null이거나, 0보다 작거나, 최대 가격보다 크거나, 소수 둘째 자리를 초과하면 잘못된 가격으로 처리.
    // 소수 자릿수를 확인하지 않으면 저장 시 컬럼 스케일(2)에 맞춰 조용히 반올림되어 응답값과 실제 저장값이 달라질 수 있음
    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(MAX_PRICE) > 0
                || price.scale() > MAX_PRICE_SCALE) {
            throw new BusinessException(ErrorCode.INVALID_PRICE);
        }
    }
}