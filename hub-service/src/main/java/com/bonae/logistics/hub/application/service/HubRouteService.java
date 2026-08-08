package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteCreateRequest;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteListItemResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRouteService {

    private static final String HUB_ROUTE_PAIR_UNIQUE_CONSTRAINT = "uk_p_hub_routes_active_pair";

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;

    @Transactional
    public HubRouteDetailResponse create(HubRouteCreateRequest request) {
        Hub departureHub = findActiveHub(request.getDepartureHubId());
        Hub arrivalHub = findActiveHub(request.getArrivalHubId());

        // 1차 방어: 활성 데이터 기준 동일 방향 중복 검사
        if (hubRouteRepository.existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
                departureHub.getId(), arrivalHub.getId())) {
            throw new BusinessException(ErrorCode.HUB_ROUTE_DUPLICATED);
        }

        HubRoute hubRoute = HubRoute.create(departureHub, arrivalHub);

        // 최종 방어: DB 부분 Unique 인덱스
        try {
            hubRouteRepository.saveAndFlush(hubRoute);
        } catch (DataIntegrityViolationException e) {
            throw convertDuplicateException(e);
        }

        return HubRouteDetailResponse.from(hubRoute);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<HubRouteListItemResponse> getHubRoutes(PageRequestDto pageRequestDto, String keyword) {
        Page<HubRoute> hubRoutes = hubRouteRepository.findAllByKeywordAndDeletedAtIsNull(normalizeKeyword(keyword), pageRequestDto.toPageable());
        return PageResponseDto.from(hubRoutes, HubRouteListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public HubRouteDetailResponse getHubRouteDetail(UUID hubRouteId) {
        HubRoute hubRoute = hubRouteRepository.findByIdAndDeletedAtIsNull(hubRouteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));
        return HubRouteDetailResponse.from(hubRoute);
    }

    @Transactional
    public HubRouteDetailResponse update(UUID hubRouteId, HubRouteUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        HubRoute hubRoute = hubRouteRepository.findByIdAndDeletedAtIsNull(hubRouteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));

        Integer distanceMeters = request.getDistanceMeters() != null ? request.getDistanceMeters() : hubRoute.getDistanceMeters();
        Integer durationSeconds = request.getDurationSeconds() != null ? request.getDurationSeconds() : hubRoute.getDurationSeconds();

        hubRoute.update(distanceMeters, durationSeconds);

        return HubRouteDetailResponse.from(hubRoute);
    }

    private Hub findActiveHub(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
    }

    private RuntimeException convertDuplicateException(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve
                && HUB_ROUTE_PAIR_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName())) {
            return new BusinessException(ErrorCode.HUB_ROUTE_DUPLICATED);
        }
        return e;
    }

    private String normalizeKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.strip();
    }

}
