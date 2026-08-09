package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Delivery> findByIdAndReceiverCompanyIdAndDeletedAtIsNull(UUID id, UUID receiverCompanyId);

    Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Delivery> findAllByReceiverCompanyIdAndDeletedAtIsNull(UUID receiverCompanyId, Pageable pageable);
}
