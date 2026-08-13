package com.bonae.logistics.company.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 같은 (order_id, product_id, operation) 조합의 재고 차감/복구 재요청을 막기 위한 멱등성 키.
// 멱등성 키 선점은 InventoryIdempotencyKeyRepository.tryInsert(네이티브 INSERT ... ON CONFLICT DO NOTHING)로만 이루어진다.
// beforeQuantity/afterQuantity는 실제로 재고에 반영해봐야 아는 값이라 선점 시점엔 비워두고, 반영 직후 같은
// 트랜잭션 안에서 fillSnapshot으로 채운다. 재요청 시엔 이 스냅샷을 그대로 돌려줘 그 사이 다른 주문이 같은
// 재고를 건드렸어도 항상 "이 주문이 실제로 한 일"을 일관되게 응답한다. quantity는 저장하지 않고 매 요청의
// 값을 그대로 응답에 반영한다(응답 DTO에만 존재).
@Entity
@Getter
@Table(name = "p_inventory_idempotency_keys", schema = "company_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryIdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 10, updatable = false)
    private InventoryChangeType operation;

    // 최초 처리 시점의 변경 전/후 재고 스냅샷. 재요청 시 재고를 다시 조회하지 않고 이 값을 그대로 반환한다.
    @Column(name = "before_quantity")
    private Integer beforeQuantity;

    @Column(name = "after_quantity")
    private Integer afterQuantity;

    //created_at은 삽입 쿼리의 CURRENT_TIMESTAMP로 채움(네이티브 쿼리는 Hibernate 생명주기를 거치지 않아 JPA Auditing이 적용되지 않음)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
