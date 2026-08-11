package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.infrastructure.client.CompanyClient;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.CompanyInfoClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
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
    private final CompanyClient companyClient;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId, UserRole userRole, UUID companyId, String username) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, companyId, username);
        return DeliveryDetailResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<DeliveryListItemResponse> getDeliveries(PageRequestDto pageRequestDto, UserRole userRole, UUID companyId, String username) {
        Page<Delivery> deliveries = switch (userRole) {
            case MASTER -> deliveryRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
            case COMPANY_MANAGER -> deliveryRepository.findAllByReceiverCompanyIdAndDeletedAtIsNull(
                    requireCompanyId(companyId), pageRequestDto.toPageable()
            );
            case HUB_MANAGER -> deliveryRepository.findAllByHubIdAndDeletedAtIsNull(
                    requireHubId(username), pageRequestDto.toPageable()
            );
            case DELIVERY_MANAGER -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
        return PageResponseDto.from(deliveries, DeliveryListItemResponse::from);
    }

    @Transactional
    public DeliveryCreateResponse createDelivery(DeliveryCreateRequest request) {
        CompanyInfoClientResponse supplierCompany = companyClient.getCompany(request.getSupplierCompanyId());
        CompanyInfoClientResponse receiverCompany = companyClient.getCompany(request.getReceiverCompanyId());
        UserInfoClientResponse receiverUser = userClient.getUserInfo(request.getReceiverUsername());

        validateCompanyMapping(supplierCompany, receiverCompany);

        Delivery delivery = Delivery.create(
                request.getOrderId(),
                supplierCompany.getHubId(),
                receiverCompany.getHubId(),
                request.getReceiverCompanyId(),
                receiverUser.getName(),
                receiverUser.getSlackId(),
                receiverCompany.getAddress()
        );

        Delivery savedDelivery = deliveryRepository.saveAndFlush(delivery);
        return DeliveryCreateResponse.from(savedDelivery);
    }

    @Transactional
    public DeliveryCancelResponse cancelDelivery(UUID deliveryId, UserRole userRole, String username) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, null, username);

        delivery.cancel();
        deliveryRepository.flush();
        return DeliveryCancelResponse.from(delivery);
    }

    private Delivery findDeliveryByRole(UUID deliveryId, UserRole userRole, UUID companyId, String username) {
        if (userRole == UserRole.COMPANY_MANAGER) {
            return deliveryRepository.findByIdAndReceiverCompanyIdAndDeletedAtIsNull(deliveryId, requireCompanyId(companyId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        }
        if (userRole == UserRole.DELIVERY_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (userRole == UserRole.HUB_MANAGER) {
            UUID hubId = requireHubId(username);
            if (!hubId.equals(delivery.getOriginHubId()) && !hubId.equals(delivery.getDestinationHubId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        return delivery;
    }

    private UUID requireCompanyId(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return companyId;
    }

    private UUID requireHubId(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        UserInfoClientResponse userInfo = userClient.getUserInfo(username);
        if (userInfo.getHubId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userInfo.getHubId();
    }

    private void validateCompanyMapping(CompanyInfoClientResponse supplierCompany, CompanyInfoClientResponse receiverCompany) {
        if (supplierCompany.getHubId() == null || receiverCompany.getHubId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업체 허브 정보가 누락되어 배송 경로를 계산할 수 없습니다.");
        }

        String receiverAddress = receiverCompany.getAddress();
        if (receiverAddress == null || receiverAddress.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수령 업체 주소가 누락되어 배송지를 생성할 수 없습니다.");
        }
    }
}
