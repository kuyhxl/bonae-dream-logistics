package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    boolean existsByProduct_IdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);

    // productId/hubId 모두 null이면 조건 없이 전체 조회, 값이 있으면 해당 조건으로 필터링 (재고 검색용)
    @Query("SELECT i FROM Inventory i WHERE i.deletedAt IS NULL "
            + "AND (:productId IS NULL OR i.product.id = :productId) "
            + "AND (:hubId IS NULL OR i.hubId = :hubId)")
    Page<Inventory> searchByProductIdAndHubIdAndDeletedAtIsNull(@Param("productId") UUID productId,
                                                                  @Param("hubId") UUID hubId,
                                                                  Pageable pageable);
}