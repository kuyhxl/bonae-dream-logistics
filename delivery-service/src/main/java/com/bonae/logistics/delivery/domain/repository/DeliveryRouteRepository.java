package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    Optional<DeliveryRoute> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    boolean existsByDeliveryIdAndDeliveryManagerIdAndDeletedAtIsNull(UUID deliveryId, UUID deliveryManagerId);

    boolean existsByDeliveryIdAndSequenceNoAndRouteStatusAndDeletedAtIsNull(
            UUID deliveryId,
            Integer sequenceNo,
            RouteStatus routeStatus
    );

    @Query("""
            select r.deliveryManagerId
            from DeliveryRoute r
            join DeliveryManager dm on dm.id = r.deliveryManagerId
            where r.deletedAt is null
              and dm.deletedAt is null
              and dm.managerType = :managerType
              and r.deliveryManagerId is not null
            order by r.createdAt desc, r.sequenceNo desc
            """)
    List<UUID> findRecentAssignedRouteManagerIdsByManagerType(
            @Param("managerType") ManagerType managerType,
            Pageable pageable
    );
}
