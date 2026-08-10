package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
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

    @Transactional
    public DeliveryCreateResponse createDelivery(DeliveryCreateRequest request) {
        // Company/User/Hub internal API orchestration is agreed in the contract
        // but intentionally deferred to a follow-up PR to keep this PR as a skeleton.
        throw new BusinessException(
                ErrorCode.SERVICE_UNAVAILABLE,
                "배송 생성 오케스트레이션은 후속 PR에서 구현 예정입니다."
        );
    }

    @Transactional
    public DeliveryCancelResponse cancelDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.cancel();
        deliveryRepository.flush();
        return DeliveryCancelResponse.from(delivery);
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
