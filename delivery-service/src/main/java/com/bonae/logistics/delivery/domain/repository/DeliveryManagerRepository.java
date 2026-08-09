package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID id);

    Page<DeliveryManager> findAllByDeletedAtIsNull(Pageable pageable);
}
