package com.bonae.logistics.hub.domain.repository;

import com.bonae.logistics.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<Hub, UUID> {
}
