package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRoutePathProvider {

    private static final String HUB_PATH_CACHE = "hubPath";

    private final HubRouteGraphProvider hubRouteGraphProvider;
    private final HubRoutePathFinder hubRoutePathFinder;

    @Cacheable(cacheNames = HUB_PATH_CACHE, key = "#departureHubId + ':' + #arrivalHubId")
    public HubRoutePathResponse getShortestPath(UUID departureHubId, UUID arrivalHubId) {
        List<HubRouteEdge> activeRoutes = hubRouteGraphProvider.getActiveRoutes();
        List<HubRouteEdge> path = hubRoutePathFinder.findShortestPath(activeRoutes, departureHubId, arrivalHubId);
        return HubRoutePathResponse.from(path);
    }

}
