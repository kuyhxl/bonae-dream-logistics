package com.bonae.logistics.delivery.domain.repository;

import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    Optional<DeliveryRoute> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);

    boolean existsByDeliveryIdAndSequenceNoAndRouteStatusAndDeletedAtIsNull(
            UUID deliveryId,
            Integer sequenceNo,
            RouteStatus routeStatus
    );
}
