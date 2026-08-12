package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    boolean existsByProduct_IdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

    Optional<Inventory> findByIdAndDeletedAtIsNull(UUID id);

    // 비관적/낙관적 락 없이 WHERE 절의 재고 조건으로 원자적으로 차감한다.
    // quantity >= :quantity를 만족하지 못해 0행이 갱신되면 재고 부족(STOCK_SHORTAGE)으로 판단한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity, i.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE i.id = :inventoryId AND i.quantity >= :quantity AND i.deletedAt IS NULL")
    int decreaseQuantity(@Param("inventoryId") UUID inventoryId, @Param("quantity") int quantity);

    // 위와 동일한 방식의 원자적 복구. 대상 재고가 없거나 이미 삭제된 경우에만 0행이 갱신된다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity, i.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE i.id = :inventoryId AND i.deletedAt IS NULL")
    int increaseQuantity(@Param("inventoryId") UUID inventoryId, @Param("quantity") int quantity);
}