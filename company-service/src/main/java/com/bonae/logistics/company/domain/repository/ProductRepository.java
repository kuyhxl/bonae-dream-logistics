package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
}