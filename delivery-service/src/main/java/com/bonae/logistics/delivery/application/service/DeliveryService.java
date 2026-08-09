package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId, UserRole userRole, UUID companyId) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, companyId);
        return DeliveryDetailResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<DeliveryListItemResponse> getDeliveries(PageRequestDto pageRequestDto, UserRole userRole, UUID companyId) {
        Page<Delivery> deliveries = userRole == UserRole.COMPANY_MANAGER
                ? deliveryRepository.findAllByReceiverCompanyIdAndDeletedAtIsNull(requireCompanyId(companyId), pageRequestDto.toPageable())
                : deliveryRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
        return PageResponseDto.from(deliveries, DeliveryListItemResponse::from);
    }

    private Delivery findDeliveryByRole(UUID deliveryId, UserRole userRole, UUID companyId) {
        if (userRole == UserRole.COMPANY_MANAGER) {
            return deliveryRepository.findByIdAndReceiverCompanyIdAndDeletedAtIsNull(deliveryId, requireCompanyId(companyId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        }

        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private UUID requireCompanyId(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return companyId;
    }
}
