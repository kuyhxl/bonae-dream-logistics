package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.domain.delivery.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.presentation.dto.request.ReqCreateDeliveryManagerDto;
import com.bonae.logistics.delivery.presentation.dto.request.ReqUpdateDeliveryManagerDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResDeliveryManagerDto;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManagerService {

    private static final String HUB_DELIVERY_SEQUENCE_UNIQUE_INDEX = "uk_p_delivery_managers_active_hub_delivery_sequence";
    private static final String COMPANY_DELIVERY_SEQUENCE_UNIQUE_INDEX = "uk_p_delivery_managers_active_company_delivery_sequence";

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final AuditorAware<String> auditorAware;

    @Transactional
    public ResDeliveryManagerDto createDeliveryManager(ReqCreateDeliveryManagerDto reqDto) {
        DeliveryManager deliveryManager = DeliveryManager.create(
                UUID.randomUUID(),
                reqDto.getHubId(),
                reqDto.getManagerType(),
                reqDto.getDeliverySequence()
        );

        try {
            DeliveryManager savedDeliveryManager = deliveryManagerRepository.saveAndFlush(deliveryManager);
            return ResDeliveryManagerDto.from(savedDeliveryManager);
        } catch (DataIntegrityViolationException e) {
            throwDuplicateSequenceIfMatched(e);
            throw e;
        }
    }

    public ResDeliveryManagerDto getDeliveryManager(UUID deliveryManagerId) {
        return ResDeliveryManagerDto.from(findActiveDeliveryManager(deliveryManagerId));
    }

    public PageResponseDto<ResDeliveryManagerDto> getDeliveryManagers(PageRequestDto pageRequestDto) {
        Page<DeliveryManager> page = deliveryManagerRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
        return PageResponseDto.from(page, ResDeliveryManagerDto::from);
    }

    @Transactional
    public ResDeliveryManagerDto updateDeliveryManager(UUID deliveryManagerId, ReqUpdateDeliveryManagerDto reqDto) {
        DeliveryManager deliveryManager = findActiveDeliveryManager(deliveryManagerId);
        deliveryManager.update(
                reqDto.getHubId(),
                reqDto.getManagerType(),
                reqDto.getDeliverySequence()
        );

        try {
            deliveryManagerRepository.flush();
            return ResDeliveryManagerDto.from(deliveryManager);
        } catch (DataIntegrityViolationException e) {
            throwDuplicateSequenceIfMatched(e);
            throw e;
        }
    }

    @Transactional
    public void deleteDeliveryManager(UUID deliveryManagerId) {
        DeliveryManager deliveryManager = findActiveDeliveryManager(deliveryManagerId);
        deliveryManager.delete(auditorAware.getCurrentAuditor().orElse("SYSTEM"));
    }

    private DeliveryManager findActiveDeliveryManager(UUID deliveryManagerId) {
        return deliveryManagerRepository.findByIdAndDeletedAtIsNull(deliveryManagerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_FOUND));
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
