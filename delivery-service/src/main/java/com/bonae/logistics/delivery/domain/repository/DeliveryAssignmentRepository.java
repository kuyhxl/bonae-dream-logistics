package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    List<DeliveryAssignment> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    Optional<DeliveryAssignment> findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(UUID deliveryId);

    Optional<DeliveryAssignment> findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
            UUID deliveryId,
            AssignmentStatus assignmentStatus
    );

    @Query("""
            select a.deliveryManagerId
            from DeliveryAssignment a
            join DeliveryManager dm on dm.id = a.deliveryManagerId
            where a.deletedAt is null
              and dm.deletedAt is null
              and dm.managerType = :managerType
              and dm.hubId = :hubId
            order by a.assignedAt desc, a.sequenceNo desc
            """)
    List<UUID> findRecentAssignedManagerIds(
            @Param("hubId") UUID hubId,
            @Param("managerType") ManagerType managerType,
            Pageable pageable
    );
}
