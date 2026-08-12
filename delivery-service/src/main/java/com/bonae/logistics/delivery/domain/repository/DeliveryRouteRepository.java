package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
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
            where r.deletedAt is null
              and r.deliveryManagerId is not null
              and r.fromHubId = :fromHubId
            order by r.createdAt desc, r.sequenceNo desc
            """)
    List<UUID> findRecentAssignedRouteManagerIds(
            @Param("fromHubId") UUID fromHubId,
            Pageable pageable
    );
}
