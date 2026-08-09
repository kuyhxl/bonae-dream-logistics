package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional
    public DeliveryCreateResponse createDelivery(DeliveryCreateRequest request) {
        Delivery delivery = Delivery.create(
                request.getOrderId(),
                request.getOriginHubId(),
                request.getDestinationHubId(),
                request.getReceiverCompanyId(),
                request.getReceiverName(),
                request.getReceiverSlackId(),
                request.getDeliveryAddress()
        );

        Delivery savedDelivery = deliveryRepository.saveAndFlush(delivery);
        return DeliveryCreateResponse.from(savedDelivery);
    }

    @Transactional
    public DeliveryCancelResponse cancelDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.cancel();
        deliveryRepository.flush();
        return DeliveryCancelResponse.from(delivery);
    }
}
