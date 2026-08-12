package com.bonae.logistics.order.domain.repository;

import com.bonae.logistics.order.domain.entity.Order;
import com.bonae.logistics.order.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT o FROM Order o
        WHERE o.deletedAt IS NULL 
        AND (:status IS NULL OR o.status = :status)
        AND (:scopeHubId IS NULL OR o.hubId = :scopeHubId)
        AND (:scopeUserId IS NULL OR o.createdBy = :scopeUserId)
        """)
    Page<Order> search(
            @Param("status")OrderStatus status,
            @Param("scopeHubId") UUID scopeHubId,
            @Param("scopeUserId") String scopeUserId,
            Pageable pageable
            );
}
