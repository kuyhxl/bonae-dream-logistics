package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByNameAndCompany_IdAndDeletedAtIsNull(String name, UUID companyId);
}
