package com.bonae.logistics.company.domain.repository;

import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByNameAndAddressAndDeletedAtIsNull(String name, String address);

    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);

    // 자기 자신(id)은 제외하고 동일한 이름+주소를 가진 삭제되지 않은 업체가 있는지 확인 (수정 시 중복 검증용)
    boolean existsByNameAndAddressAndDeletedAtIsNullAndIdNot(String name, String address, UUID id);

    // type이 null이면 전체 조회, 값이 있으면 해당 유형만 조회
    @Query("SELECT c FROM Company c WHERE c.deletedAt IS NULL AND (:type IS NULL OR c.type = :type)")
    Page<Company> findAllByTypeAndDeletedAtIsNull(@Param("type") CompanyType type, Pageable pageable);

    // namePattern/type/hubId 모두 null이면 조건 없이 전체 조회, 값이 있으면 해당 조건으로 필터링 (업체 검색용)
    // namePattern은 호출 측에서 이미 '%keyword%' 형태로 만들어서 넘김
    // JPQL의 CONCAT('%', :keyword, '%')로 직접 만들면 keyword가 null일 때 Hibernate가 파라미터 타입을
    // 잘못 추론해 "character varying ~~ bytea" 에러가 나는 문제가 있어 이 방식을 피했다.
    @Query("SELECT c FROM Company c WHERE c.deletedAt IS NULL "
            + "AND (:namePattern IS NULL OR c.name LIKE :namePattern) "
            + "AND (:type IS NULL OR c.type = :type) "
            + "AND (:hubId IS NULL OR c.hubId = :hubId)")
    Page<Company> searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(@Param("namePattern") String namePattern,
                                                                    @Param("type") CompanyType type,
                                                                    @Param("hubId") UUID hubId,
                                                                    Pageable pageable);
}
