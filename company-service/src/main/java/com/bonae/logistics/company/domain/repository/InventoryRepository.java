package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    boolean existsByProduct_IdAndHubIdAndDeletedAtIsNull(UUID productId, UUID hubId);
}