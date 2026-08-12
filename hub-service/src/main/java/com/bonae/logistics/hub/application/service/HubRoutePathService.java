package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRoutePathService {

    private final HubRepository hubRepository;
    private final HubRoutePathProvider hubRoutePathProvider;

    public HubRoutePathResponse findShortestPath(UUID departureHubId, UUID arrivalHubId) {
        requireHubExists(departureHubId);

        // 동일 허브는 존재 여부만 검증하고 경로 결과를 캐싱하지 않는다.
        if (departureHubId.equals(arrivalHubId)) {
            return HubRoutePathResponse.from(List.of());
        }

        requireHubExists(arrivalHubId);

        // 허브 존재/활성 검증을 마친 뒤에만 경로 결과 캐시를 조회한다.
        return hubRoutePathProvider.getShortestPath(departureHubId, arrivalHubId);
    }

    private void requireHubExists(UUID hubId) {
        if (!hubRepository.existsByIdAndDeletedAtIsNull(hubId)) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
    }
}