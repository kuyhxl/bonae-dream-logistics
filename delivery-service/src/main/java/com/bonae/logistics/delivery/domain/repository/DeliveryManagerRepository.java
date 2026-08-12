package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {

    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID id);

    Page<DeliveryManager> findAllByDeletedAtIsNull(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DeliveryManager> findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            UUID hubId,
            ManagerType managerType
    );
}
