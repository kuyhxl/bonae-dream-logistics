package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByNameAndCompany_IdAndDeletedAtIsNull(String name, UUID companyId);

    // namePattern/companyId 모두 null이면 조건 없이 전체 조회, 값이 있으면 해당 조건으로 필터링 (상품 검색용)
    // namePattern은 호출 측에서 이미 '%keyword%' 형태로 만들어서 넘김
    // (CONCAT을 JPQL에서 직접 쓰면 keyword가 null일 때 Hibernate가 파라미터 타입을 잘못 추론하는 문제가 있어 이 방식을 피함)
    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL "
            + "AND (:namePattern IS NULL OR p.name LIKE :namePattern) "
            + "AND (:companyId IS NULL OR p.company.id = :companyId)")
    Page<Product> searchByKeywordAndCompanyIdAndDeletedAtIsNull(@Param("namePattern") String namePattern,
                                                                 @Param("companyId") UUID companyId,
                                                                 Pageable pageable);
}
