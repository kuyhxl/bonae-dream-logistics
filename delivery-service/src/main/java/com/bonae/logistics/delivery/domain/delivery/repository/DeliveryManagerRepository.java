package com.bonae.logistics.delivery.domain.delivery.repository;

import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByDeliveryManagerIdAndDeletedAtIsNull(UUID deliveryManagerId);

    Page<DeliveryManager> findAllByDeletedAtIsNull(Pageable pageable);
}
