package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubRouteGraphProvider {

    private static final String HUB_ROUTE_GRAPH_CACHE = "hubRouteGraph";

    private final HubRouteRepository hubRouteRepository;

    @Cacheable(cacheNames = HUB_ROUTE_GRAPH_CACHE, key = "'active'")
    @Transactional(readOnly = true)
    public List<HubRouteEdge> getActiveRoutes() {
        return hubRouteRepository.findAllByDeletedAtIsNull().stream()
                .map(HubRouteEdge::from)
                .toList();
    }
}