package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByNameAndAddressAndDeletedAtIsNull(String name, String address);

    // type이 null이면 전체 조회, 값이 있으면 해당 유형만 조회
    @Query("SELECT c FROM Company c WHERE c.deletedAt IS NULL AND (:type IS NULL OR c.type = :type)")
    Page<Company> findAllByTypeAndDeletedAtIsNull(@Param("type") CompanyType type, Pageable pageable);
}
