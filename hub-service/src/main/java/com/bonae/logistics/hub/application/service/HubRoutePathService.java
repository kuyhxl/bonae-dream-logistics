package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRoutePathService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final HubRoutePathFinder hubRoutePathFinder;

    @Transactional(readOnly = true)
    public HubRoutePathResponse findShortestPath(UUID departureHubId, UUID arrivalHubId) {
        requireHubExists(departureHubId);

        // 동일 허브는 존재 여부만 검증하고 활성 간선을 조회하지 않는다.
        if (departureHubId.equals(arrivalHubId)) {
            return HubRoutePathResponse.from(List.of());
        }

        requireHubExists(arrivalHubId);

        // JPA 엔티티와 경로 탐색 입력을 분리하기 위해 활성 간선을 불변 값 객체로 변환한다.
        List<HubRouteEdge> activeRoutes = hubRouteRepository.findAllByDeletedAtIsNull().stream()
                .map(HubRouteEdge::from)
                .toList();

        List<HubRouteEdge> path = hubRoutePathFinder.findShortestPath(activeRoutes, departureHubId, arrivalHubId);
        return HubRoutePathResponse.from(path);
    }

    private void requireHubExists(UUID hubId) {
        if (!hubRepository.existsByIdAndDeletedAtIsNull(hubId)) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
    }
}