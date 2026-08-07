package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubService {

    private static final String HUB_NAME_UNIQUE_CONSTRAINT = "uk_p_hubs_active_name";
    private static final String HUB_ADDRESS_UNIQUE_CONSTRAINT = "uk_p_hubs_active_address";

    private final HubRepository hubRepository;

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
    public HubResponse getHub(UUID hubId) {
        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        return HubResponse.from(hub);
    }

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
