package com.bonae.logistics.delivery.domain.delivery.repository;

import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable);
}
