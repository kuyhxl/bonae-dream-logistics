package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
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
    private final HubRoutePathFinder hubRoutePathFinder;

    @Transactional(readOnly = true)
    public HubRoutePathResponse findShortestPath(UUID departureHubId, UUID arrivalHubId) {
        requireHubExists(departureHubId);

        if (!departureHubId.equals(arrivalHubId)) {
            requireHubExists(arrivalHubId);
        }

        List<HubRoute> path = hubRoutePathFinder.findShortestPath(departureHubId, arrivalHubId);
        return HubRoutePathResponse.from(path);
    }

    private void requireHubExists(UUID hubId) {
        if (!hubRepository.existsByIdAndDeletedAtIsNull(hubId)) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
    }
}