package com.bonae.logistics.delivery.domain.delivery.repository;

import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(UUID deliveryId);
}
