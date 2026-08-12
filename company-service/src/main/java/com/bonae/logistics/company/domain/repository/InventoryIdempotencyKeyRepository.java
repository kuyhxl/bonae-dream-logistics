package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.InventoryChangeType;
import com.bonae.logistics.company.domain.entity.InventoryIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryIdempotencyKeyRepository extends JpaRepository<InventoryIdempotencyKey, UUID> {

    //<멱등성 키 선점하기>
    // 유니크 인덱스(order_id, product_id, operation) 덕분에 이미 같은 조합이 있으면 예외 없이 0행 삽입으로 끝남.
    // 반환값이 1이면 이번이 최초 요청, 0이면 이미 처리된 (주문, 상품, 작업) 조합의 재요청이라는 뜻.
    // before_quantity/after_quantity는 실제 반영해봐야 아는 값이라 이 시점엔 비워둔다(NULL) - 반영 직후 fillSnapshot으로 채운다.
    @Modifying
    @Query(value = "INSERT INTO p_inventory_idempotency_keys (id, order_id, product_id, operation, created_at) "
            + "VALUES (:id, :orderId, :productId, :operation, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (order_id, product_id, operation) DO NOTHING", nativeQuery = true)
    int tryInsert(@Param("id") UUID id, @Param("orderId") UUID orderId,
                  @Param("productId") UUID productId, @Param("operation") String operation);

    // 선점 직후, 실제 재고 반영 결과(before/after 스냅샷)를 같은 트랜잭션 안에서 채워 넣는다.
    @Modifying
    @Query("UPDATE InventoryIdempotencyKey k SET k.beforeQuantity = :beforeQuantity, "
            + "k.afterQuantity = :afterQuantity WHERE k.id = :id")
    void fillSnapshot(@Param("id") UUID id, @Param("beforeQuantity") Integer beforeQuantity,
                       @Param("afterQuantity") Integer afterQuantity);

    // 재요청(멱등성 키 중복) 시 최초 처리 시점의 스냅샷을 조회한다.
    Optional<InventoryIdempotencyKey> findByOrderIdAndProductIdAndOperation(
            UUID orderId, UUID productId, InventoryChangeType operation);
}
