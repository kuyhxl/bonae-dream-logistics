package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRouteRepository;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRouteService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<DeliveryRouteResponse> getDeliveryRoutes(UUID deliveryId, UserRole userRole, String username) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        validateDeliveryAccess(delivery, userRole, username);

        return deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId).stream()
                .map(DeliveryRouteResponse::from)
                .toList();
    }

    @Transactional
    public DeliveryRouteResponse updateRouteStatus(
            UUID routeId,
            RouteStatus routeStatus,
            UserRole userRole,
            String username
    ) {
        DeliveryRoute deliveryRoute = deliveryRouteRepository.findByIdAndDeletedAtIsNull(routeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        validateRouteAccess(deliveryRoute, userRole, username);
        Delivery delivery = validateParentDeliveryStatus(deliveryRoute.getDeliveryId());

        if (routeStatus == RouteStatus.IN_TRANSIT) {
            validateRouteSequenceForDeparture(deliveryRoute);
            deliveryRoute.depart();
            delivery.startHubMovement();
        } else if (routeStatus == RouteStatus.ARRIVED) {
            deliveryRoute.arrive();
            delivery.completeHubRoute(isLastRoute(deliveryRoute));
        } else {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        return DeliveryRouteResponse.from(deliveryRoute);
    }

    private boolean hasRouteAccess(DeliveryRoute deliveryRoute, UserRole userRole, String username) {
        UserInfoClientResponse userInfo = requiresUserInfo(userRole) ? getRequiredUserInfo(username) : null;
        return hasRouteAccess(deliveryRoute, userRole, userInfo);
    }

    private boolean hasRouteAccess(DeliveryRoute deliveryRoute, UserRole userRole, UserInfoClientResponse userInfo) {
        return switch (userRole) {
            case MASTER -> true;
            case HUB_MANAGER -> matchesHub(deliveryRoute, requireHubId(userInfo));
            case DELIVERY_MANAGER -> matchesDeliveryManager(deliveryRoute, requireUserId(userInfo));
            case COMPANY_MANAGER -> false;
        };
    }

    private void validateRouteAccess(DeliveryRoute deliveryRoute, UserRole userRole, String username) {
        UserInfoClientResponse userInfo = requiresUserInfo(userRole) ? getRequiredUserInfo(username) : null;
        if (!hasRouteAccess(deliveryRoute, userRole, userInfo)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateDeliveryAccess(Delivery delivery, UserRole userRole, String username) {
        UserInfoClientResponse userInfo = requiresUserInfo(userRole) ? getRequiredUserInfo(username) : null;

        boolean allowed = switch (userRole) {
            case MASTER -> true;
            case HUB_MANAGER -> matchesHub(delivery, requireHubId(userInfo));
            case DELIVERY_MANAGER -> matchesDeliveryManager(delivery, requireUserId(userInfo));
            case COMPANY_MANAGER -> false;
        };

        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Delivery validateParentDeliveryStatus(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_CANCELLED);
        }

        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_COMPLETED);
        }

        return delivery;
    }

    private void validateRouteSequenceForDeparture(DeliveryRoute deliveryRoute) {
        if (deliveryRoute.getSequenceNo() == null || deliveryRoute.getSequenceNo() <= 1) {
            return;
        }

        boolean previousArrived = deliveryRouteRepository.existsByDeliveryIdAndSequenceNoAndRouteStatusAndDeletedAtIsNull(
                deliveryRoute.getDeliveryId(),
                deliveryRoute.getSequenceNo() - 1,
                RouteStatus.ARRIVED
        );

        if (!previousArrived) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private boolean isLastRoute(DeliveryRoute deliveryRoute) {
        List<DeliveryRoute> routes = deliveryRouteRepository
                .findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryRoute.getDeliveryId());

        if (routes.isEmpty()) {
            return false;
        }

        return routes.get(routes.size() - 1).getId().equals(deliveryRoute.getId());
    }

    private boolean matchesHub(DeliveryRoute deliveryRoute, UUID hubId) {
        return hubId.equals(deliveryRoute.getFromHubId()) || hubId.equals(deliveryRoute.getToHubId());
    }

    private boolean matchesHub(Delivery delivery, UUID hubId) {
        return hubId.equals(delivery.getOriginHubId()) || hubId.equals(delivery.getDestinationHubId());
    }

    private boolean matchesDeliveryManager(DeliveryRoute deliveryRoute, UUID userId) {
        return deliveryRoute.getDeliveryManagerId() != null && userId.equals(deliveryRoute.getDeliveryManagerId());
    }

    private boolean matchesDeliveryManager(Delivery delivery, UUID userId) {
        return delivery.getDeliveryManagerId() != null && userId.equals(delivery.getDeliveryManagerId());
    }

    private UUID requireHubId(UserInfoClientResponse userInfo) {
        if (userInfo.getHubId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userInfo.getHubId();
    }

    private UUID requireUserId(UserInfoClientResponse userInfo) {
        if (userInfo.getId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userInfo.getId();
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

    private boolean requiresUserInfo(UserRole userRole) {
        return userRole == UserRole.HUB_MANAGER || userRole == UserRole.DELIVERY_MANAGER;
    }
}
