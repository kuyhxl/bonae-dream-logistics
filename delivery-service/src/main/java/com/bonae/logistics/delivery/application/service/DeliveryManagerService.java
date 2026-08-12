package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerSearchRequest;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryManagerUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryManagerResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerService {

    private static final String HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX = "uk_p_delivery_managers_active_hub_delivery_sequence";
    private static final String COMPANY_DELIVERY_SEQUENCE_UNIQUE_INDEX = "uk_p_delivery_managers_active_company_delivery_sequence";

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final CurrentAuditorProvider currentAuditorProvider;

    @Transactional
    public DeliveryManagerResponse createDeliveryManager(DeliveryManagerCreateRequest reqDto) {
        DeliveryManager deliveryManager = DeliveryManager.create(
                reqDto.getDeliveryManagerId(),
                reqDto.getHubId(),
                reqDto.getManagerType(),
                reqDto.getDeliverySequence()
        );

        try {
            DeliveryManager savedDeliveryManager = deliveryManagerRepository.saveAndFlush(deliveryManager);
            return DeliveryManagerResponse.from(savedDeliveryManager);
        } catch (DataIntegrityViolationException e) {
            throwDuplicateSequenceIfMatched(e);
            throw e;
        }
    }

    public DeliveryManagerResponse getDeliveryManager(UUID deliveryManagerId) {
        return DeliveryManagerResponse.from(findActiveDeliveryManager(deliveryManagerId));
    }

    public PageResponseDto<DeliveryManagerResponse> getDeliveryManagers(PageRequestDto pageRequestDto) {
        Page<DeliveryManager> page = deliveryManagerRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
        return PageResponseDto.from(page, DeliveryManagerResponse::from);
    }

    public PageResponseDto<DeliveryManagerResponse> searchDeliveryManagers(
            PageRequestDto pageRequestDto,
            DeliveryManagerSearchRequest searchRequest
    ) {
        Page<DeliveryManager> page = deliveryManagerRepository.findAll(
                buildSearchSpecification(searchRequest),
                pageRequestDto.toPageable()
        );
        return PageResponseDto.from(page, DeliveryManagerResponse::from);
    }

    @Transactional
    public DeliveryManagerResponse updateDeliveryManager(UUID deliveryManagerId, DeliveryManagerUpdateRequest reqDto) {
        DeliveryManager deliveryManager = findActiveDeliveryManager(deliveryManagerId);
        deliveryManager.update(
                reqDto.getHubId(),
                reqDto.getManagerType(),
                reqDto.getDeliverySequence()
        );

        try {
            deliveryManagerRepository.flush();
            return DeliveryManagerResponse.from(deliveryManager);
        } catch (DataIntegrityViolationException e) {
            throwDuplicateSequenceIfMatched(e);
            throw e;
        }
    }

    @Transactional
    public void deleteDeliveryManager(UUID deliveryManagerId) {
        DeliveryManager deliveryManager = findActiveDeliveryManager(deliveryManagerId);
        deliveryManager.delete(currentAuditorProvider.getCurrentAuditorOrSystem());
    }

    private DeliveryManager findActiveDeliveryManager(UUID deliveryManagerId) {
        return deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_FOUND));
    }

    private Specification<DeliveryManager> buildSearchSpecification(DeliveryManagerSearchRequest searchRequest) {
        Specification<DeliveryManager> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));

        if (searchRequest == null) {
            return specification;
        }

        specification = specification.and(equalsHubId(searchRequest.getHubId()));
        specification = specification.and(equalsManagerType(searchRequest.getManagerType()));
        specification = specification.and(equalsDeliverySequence(searchRequest.getDeliverySequence()));
        return specification;
    }

    private Specification<DeliveryManager> equalsHubId(UUID hubId) {
        return (root, query, criteriaBuilder) ->
                hubId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("hubId"), hubId);
    }

    private Specification<DeliveryManager> equalsManagerType(ManagerType managerType) {
        return (root, query, criteriaBuilder) ->
                managerType == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("managerType"), managerType);
    }

    private Specification<DeliveryManager> equalsDeliverySequence(Integer deliverySequence) {
        return (root, query, criteriaBuilder) ->
                Objects.isNull(deliverySequence)
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.equal(root.get("deliverySequence"), deliverySequence);
    }

    private void throwDuplicateSequenceIfMatched(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            if (HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX.equalsIgnoreCase(constraintName)
                    || COMPANY_DELIVERY_SEQUENCE_UNIQUE_INDEX.equalsIgnoreCase(constraintName)) {
                throw new BusinessException(ErrorCode.DELIVERY_MANAGER_SEQUENCE_DUPLICATED);
            }
        }
    }
}
