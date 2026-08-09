package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.domain.delivery.entity.Delivery;
import com.bonae.logistics.delivery.domain.delivery.repository.DeliveryRepository;
import com.bonae.logistics.delivery.presentation.dto.request.ReqCreateDeliveryDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResCancelDeliveryDto;
import com.bonae.logistics.delivery.presentation.dto.response.ResCreateDeliveryDto;
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
    public ResCreateDeliveryDto createDelivery(ReqCreateDeliveryDto reqDto) {
        Delivery delivery = Delivery.create(
                reqDto.getOrderId(),
                reqDto.getOriginHubId(),
                reqDto.getDestinationHubId(),
                reqDto.getReceiverCompanyId(),
                reqDto.getReceiverName(),
                reqDto.getReceiverSlackId(),
                reqDto.getDeliveryAddress()
        );

        Delivery savedDelivery = deliveryRepository.saveAndFlush(delivery);
        return ResCreateDeliveryDto.from(savedDelivery);
    }

    @Transactional
    public ResCancelDeliveryDto cancelDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.cancel();
        deliveryRepository.flush();
        return ResCancelDeliveryDto.from(delivery);
    }
}
