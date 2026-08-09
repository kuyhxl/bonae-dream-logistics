package com.bonae.logistics.hub.domain.repository;

import com.bonae.logistics.hub.domain.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {
    // 삭제할 허브가 출발 또는 도착으로 연결된 활성 이동정보를 모두 조회
    @Query("SELECT r " +
            "FROM HubRoute r " +
            "WHERE r.deletedAt IS NULL " +
            "AND (r.departureHub.id = :hubId " +
            "OR r.arrivalHub.id = :hubId)")
    List<HubRoute> findAllActiveByHubId(@Param("hubId") UUID hubId);
}
