package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryAssignmentRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryAssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final UserClient userClient;

    @Transactional
    public DeliveryAssignmentResponse assignDelivery(
            UUID deliveryId,
            String reason,
            UserRole userRole,
            String username
    ) {
        Delivery delivery = getDestinationHubAssignableDelivery(deliveryId, userRole, username);
        DeliveryManager nextManager = getNextCompanyDeliveryManager(delivery, null);
        int nextSequence = deliveryAssignmentRepository.findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(deliveryId)
                .map(assignment -> assignment.getSequenceNo() + 1)
                .orElse(1);

        delivery.assignManager(nextManager.getId());
        DeliveryAssignment deliveryAssignment = DeliveryAssignment.create(
                delivery.getId(),
                nextManager.getId(),
                nextSequence,
                reason
        );

        return DeliveryAssignmentResponse.from(deliveryAssignmentRepository.save(deliveryAssignment));
    }

    @Transactional
    public DeliveryAssignmentResponse reassignDelivery(
            UUID deliveryId,
            String reason,
            UserRole userRole,
            String username
    ) {
        Delivery delivery = getDestinationHubAssignableDelivery(deliveryId, userRole, username);
        DeliveryAssignment latestAssignment = deliveryAssignmentRepository
                .findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        if (delivery.getDeliveryManagerId() == null || latestAssignment.getAssignmentStatus() != AssignmentStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        UUID currentManagerId = delivery.getDeliveryManagerId();
        DeliveryManager nextManager = getNextCompanyDeliveryManager(delivery, currentManagerId);

        latestAssignment.markReassigned(reason);
        delivery.reassignManager(nextManager.getId());

        DeliveryAssignment nextAssignment = DeliveryAssignment.create(
                delivery.getId(),
                nextManager.getId(),
                latestAssignment.getSequenceNo() + 1,
                reason
        );

        return DeliveryAssignmentResponse.from(deliveryAssignmentRepository.save(nextAssignment));
    }

    private Delivery getDestinationHubAssignableDelivery(UUID deliveryId, UserRole userRole, String username) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (userRole == UserRole.MASTER) {
            return delivery;
        }

        if (userRole != UserRole.HUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        UUID hubId = requireHubId(username);
        if (!hubId.equals(delivery.getDestinationHubId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return delivery;
    }

    private DeliveryManager getNextCompanyDeliveryManager(Delivery delivery, UUID excludedManagerId) {
        List<DeliveryManager> candidates = deliveryManagerRepository
                .findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                        delivery.getDestinationHubId(),
                        ManagerType.COMPANY_DELIVERY
                );

        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
        }

        List<UUID> recentManagerIds = deliveryAssignmentRepository.findRecentAssignedManagerIds(
                delivery.getDestinationHubId(),
                ManagerType.COMPANY_DELIVERY,
                PageRequest.of(0, 1)
        );
        UUID lastAssignedManagerId = recentManagerIds.isEmpty() ? null : recentManagerIds.get(0);

        return pickNextManager(candidates, lastAssignedManagerId, excludedManagerId);
    }

    private DeliveryManager pickNextManager(
            List<DeliveryManager> candidates,
            UUID lastAssignedManagerId,
            UUID excludedManagerId
    ) {
        int startIndex = 0;
        if (lastAssignedManagerId != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).getId().equals(lastAssignedManagerId)) {
                    startIndex = (i + 1) % candidates.size();
                    break;
                }
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            DeliveryManager candidate = candidates.get((startIndex + i) % candidates.size());
            if (excludedManagerId == null || !excludedManagerId.equals(candidate.getId())) {
                return candidate;
            }
        }

        throw new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
    }

    private UUID requireHubId(String username) {
        UserInfoClientResponse userInfo = getRequiredUserInfo(username);
        if (userInfo.getHubId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return userInfo.getHubId();
    }

    private UserInfoClientResponse getRequiredUserInfo(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        UserInfoClientResponse userInfo = userClient.getUserInfo(username);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userInfo;
    }
}
