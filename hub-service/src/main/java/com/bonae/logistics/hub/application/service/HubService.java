package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.config.AuditorAwareImpl;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import com.bonae.logistics.hub.presentation.dto.request.HubUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubListItemResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubService {

    private static final String HUB_NAME_UNIQUE_CONSTRAINT = "uk_p_hubs_active_name";
    private static final String HUB_ADDRESS_UNIQUE_CONSTRAINT = "uk_p_hubs_active_address";
    private static final String HUB_DETAIL_CACHE = "hubDetail";

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final AuditorAware<String> auditorAware;

    @Transactional
    public HubDetailResponse create(HubCreateRequest request) {
        String name = request.getName();
        String address = request.getAddress();

        // 1차 방어: 활성 데이터 기준 중복 검사
        if (hubRepository.existsByNameAndDeletedAtIsNull(name)) {
            throw new BusinessException(ErrorCode.HUB_NAME_DUPLICATED);
        }
        if (hubRepository.existsByAddressAndDeletedAtIsNull(address)) {
            throw new BusinessException(ErrorCode.HUB_ADDRESS_DUPLICATED);
        }

        Hub hub = Hub.create(name, address, request.getLatitude(), request.getLongitude());

        // 최종 방어: DB 부분 Unique 인덱스
        // 동시 요청으로 1차 검사를 통과한 경우 saveAndFlush에서 제약조건 위반이 발생하므로 중복 에러로 변환한다.
        // 그 외 제약조건 위반은 예상치 못한 오류이므로 그대로 던진다.
        try {
            hubRepository.saveAndFlush(hub);
        } catch (DataIntegrityViolationException e) {
            throw convertDuplicateException(e);
        }

        return HubDetailResponse.from(hub);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<HubListItemResponse> getHubs(PageRequestDto pageRequestDto, String keyword) {
        Page<Hub> hubs;
        if (!StringUtils.hasText(keyword)) {
            hubs = hubRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
        } else {
            hubs = hubRepository.findAllByKeywordAndDeletedAtIsNull(keyword.trim(), pageRequestDto.toPageable());
        }
        return PageResponseDto.from(hubs, HubListItemResponse::from);
    }

    // 공개 API(GET /api/hubs/{hubId})용 상세 조회
    @Cacheable(cacheNames = HUB_DETAIL_CACHE, key = "#hubId")
    @Transactional(readOnly = true)
    public HubDetailResponse getHubDetail(UUID hubId) {
        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        return HubDetailResponse.from(hub);
    }

    @CacheEvict(cacheNames = HUB_DETAIL_CACHE, key = "#hubId")
    @Transactional
    public HubDetailResponse update(UUID hubId, HubUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));

        String name = request.getName() != null ? request.getName() : hub.getName();
        String address = request.getAddress() != null ? request.getAddress() : hub.getAddress();
        Double latitude = request.getLatitude() != null ? request.getLatitude() : hub.getLatitude();
        Double longitude = request.getLongitude() != null ? request.getLongitude() : hub.getLongitude();

        if (request.getName() != null && hubRepository.existsByNameAndDeletedAtIsNullAndIdNot(name, hubId)) {
            throw new BusinessException(ErrorCode.HUB_NAME_DUPLICATED);
        }
        if (request.getAddress() != null && hubRepository.existsByAddressAndDeletedAtIsNullAndIdNot(address, hubId)) {
            throw new BusinessException(ErrorCode.HUB_ADDRESS_DUPLICATED);
        }

        hub.update(name, address, latitude, longitude);

        try {
            hubRepository.saveAndFlush(hub);
        } catch (DataIntegrityViolationException e) {
            throw convertDuplicateException(e);
        }

        return HubDetailResponse.from(hub);
    }

    @CacheEvict(cacheNames = HUB_DETAIL_CACHE, key = "#hubId")
    @Transactional
    public void delete(UUID hubId) {
        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));

        String deletedBy = auditorAware.getCurrentAuditor().orElse(AuditorAwareImpl.SYSTEM);

        hub.delete(deletedBy);

        // 연관된 이동정보도 함께 비활성화한다
        List<HubRoute> relatedRoutes = hubRouteRepository.findAllActiveByHubId(hubId);
        relatedRoutes.forEach(route -> route.delete(deletedBy));
    }

    // 내부 API(GET /api/internal/hubs/{hubId})용 존재 여부 확인, hubId만 반환
    @Transactional(readOnly = true)
    public HubResponse getHub(UUID hubId) {
        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        return HubResponse.from(hub);
    }

    // DB 제약조건명으로 중복 종류(이름/주소)를 구분해 BusinessException으로 변환 매칭 안 되면 원본 예외 그대로 반환
    private RuntimeException convertDuplicateException(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            if (HUB_NAME_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
                return new BusinessException(ErrorCode.HUB_NAME_DUPLICATED);
            }
            if (HUB_ADDRESS_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName)) {
                return new BusinessException(ErrorCode.HUB_ADDRESS_DUPLICATED);
            }
        }
        return e;
    }
}
