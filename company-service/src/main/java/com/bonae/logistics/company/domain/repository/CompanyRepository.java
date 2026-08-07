package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByNameAndAddressAndDeletedAtIsNull(String name, String address);
}
