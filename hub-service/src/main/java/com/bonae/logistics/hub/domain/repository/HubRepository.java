package com.bonae.logistics.hub.domain.repository;

import com.bonae.logistics.hub.domain.entity.Hub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {
    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    List<Hub> findAllByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    boolean existsByNameAndDeletedAtIsNull(String name);
    boolean existsByAddressAndDeletedAtIsNull(String address);

    Page<Hub> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT h
        FROM Hub h
        WHERE h.deletedAt IS NULL
          AND (
              h.name LIKE CONCAT('%', :keyword, '%')
              OR h.address LIKE CONCAT('%', :keyword, '%')
          )
        """)
    Page<Hub> findAllByKeywordAndDeletedAtIsNull(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, UUID id);
    boolean existsByAddressAndDeletedAtIsNullAndIdNot(String address, UUID id);
    boolean existsByIdAndDeletedAtIsNull(UUID id);
}

