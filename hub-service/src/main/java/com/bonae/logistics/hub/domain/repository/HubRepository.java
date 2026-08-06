package com.bonae.logistics.hub.domain.repository;

import com.bonae.logistics.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {
    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByNameAndDeletedAtIsNull(String name);
    boolean existsByAddressAndDeletedAtIsNull(String address);
}
