package com.bonae.logistics.delivery.domain.delivery.repository;

import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    List<DeliveryAssignment> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    Optional<DeliveryAssignment> findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(UUID deliveryId);
}
