package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryAssignmentRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRouteRepository;
import com.bonae.logistics.delivery.infrastructure.client.CompanyClient;
import com.bonae.logistics.delivery.infrastructure.client.HubClient;
import com.bonae.logistics.delivery.infrastructure.client.HubRouteClient;
import com.bonae.logistics.delivery.infrastructure.client.MessageClient;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.AiDispatchClientRequest;
import com.bonae.logistics.delivery.infrastructure.client.dto.CompanyInfoClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.DeliveryManagerClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.HubRoutePathClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.HubSummaryClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.InternalDeliveryUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCompleteResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final CompanyClient companyClient;
    private final HubClient hubClient;
    private final HubRouteClient hubRouteClient;
    private final MessageClient messageClient;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId, UserRole userRole, UUID companyId, String username) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, companyId, username);
        return DeliveryDetailResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<DeliveryListItemResponse> getDeliveries(
            PageRequestDto pageRequestDto,
            UserRole userRole,
            UUID companyId,
            String username
    ) {
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
        UserInfoClientResponse receiverUser = getRequiredUserInfo(request.getReceiverUsername());

        validateCompanyMapping(supplierCompany, receiverCompany);
        HubRoutePathClientResponse routePath = hubRouteClient.getShortestPath(
                supplierCompany.getHubId(),
                receiverCompany.getHubId()
        );
        List<HubRoutePathClientResponse.HubRoutePathSegmentClientResponse> routeSegments =
                validateRoutePath(supplierCompany.getHubId(), receiverCompany.getHubId(), routePath);

        Delivery delivery = Delivery.create(
                request.getOrderId(),
                supplierCompany.getHubId(),
                receiverCompany.getHubId(),
                request.getReceiverCompanyId(),
                receiverUser.getName(),
                receiverUser.getSlackId(),
                receiverCompany.getAddress(),
                request.getRequestNote()
        );
        delivery.markRoutePrepared(!routeSegments.isEmpty());

        Delivery savedDelivery = deliveryRepository.saveAndFlush(delivery);
        List<DeliveryRoute> savedRoutes = deliveryRouteRepository.saveAll(
                routeSegments.stream()
                        .map(segment -> toDeliveryRoute(savedDelivery.getId(), segment))
                        .toList()
        );
        deliveryAssignmentService.assignOnCreateSafely(savedDelivery.getId(), "배송 생성 자동 배정");
        dispatchAiDispatchSafely(savedDelivery.getId(), savedRoutes, receiverUser, request);

        return DeliveryCreateResponse.from(savedDelivery, savedRoutes.size());
    }

    @Transactional
    public DeliveryCancelResponse cancelDelivery(UUID deliveryId, UserRole userRole, String username) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, null, username);

        delivery.cancel();
        cancelLatestAssignment(delivery.getId());
        deliveryRepository.flush();
        return DeliveryCancelResponse.from(delivery);
    }

    @Transactional
    public DeliveryCancelResponse cancelDeliveryByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.cancel();
        cancelLatestAssignment(delivery.getId());
        deliveryRepository.flush();
        return DeliveryCancelResponse.from(delivery);
    }

    @Transactional
    public DeliveryCompleteResponse completeDelivery(UUID deliveryId, UserRole userRole, String username) {
        Delivery delivery = findDeliveryByRole(deliveryId, userRole, null, username);

        delivery.completeDelivery();
        requireActiveAssignment(delivery.getId()).markCompleted("배송 완료");
        deliveryRepository.flush();
        return DeliveryCompleteResponse.from(delivery);
    }

    @Transactional
    public DeliveryDetailResponse updateDeliveryByOrder(InternalDeliveryUpdateRequest request) {
        Delivery delivery = deliveryRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.updateRequestNote(request.getRequestNote());

        deliveryRepository.flush();
        return DeliveryDetailResponse.from(delivery);
    }

    private void dispatchAiDispatchSafely(
            UUID deliveryId,
            List<DeliveryRoute> savedRoutes,
            UserInfoClientResponse receiverUser,
            DeliveryCreateRequest request
    ) {
        try {
            Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
            List<DeliveryRoute> routes = deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId);
            messageClient.createAiDispatch(buildAiDispatchRequest(delivery, routes.isEmpty() ? savedRoutes : routes, receiverUser, request));
        } catch (Exception e) {
            log.warn("AI dispatch 연동 실패, 배송 생성은 유지 deliveryId={}, orderId={}",
                    deliveryId, request.getOrderId(), e);
        }
    }

    private AiDispatchClientRequest buildAiDispatchRequest(
            Delivery delivery,
            List<DeliveryRoute> routes,
            UserInfoClientResponse receiverUser,
            DeliveryCreateRequest request
    ) {
        List<UUID> intermediateHubIds = extractWaypointHubIds(routes);
        Map<UUID, String> hubNameMap = getHubNameMap(buildHubNameQueryIds(delivery.getOriginHubId(), intermediateHubIds));

        return AiDispatchClientRequest.builder()
                .orderId(delivery.getOrderId())
                .orderNo(null)
                .ordererName(receiverUser.getName())
                .ordererSlackId(receiverUser.getSlackId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .dueDate(request.getDueDate())
                .requestNote(delivery.getRequestNote())
                .originHubName(requireHubName(hubNameMap, delivery.getOriginHubId()))
                .waypointHubNames(intermediateHubIds.stream()
                        .map(hubId -> requireHubName(hubNameMap, hubId))
                        .toList())
                .destinationAddress(delivery.getDeliveryAddress())
                .totalDurationMin(calculateTotalDurationMin(routes))
                .managerSlackId(resolveManagerSlackId(delivery, routes))
                .build();
    }

    private String resolveManagerSlackId(Delivery delivery, List<DeliveryRoute> routes) {
        if (routes.isEmpty()) {
            UUID companyManagerId = delivery.getDeliveryManagerId();
            if (companyManagerId == null) {
                throw new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
            }

            return userClient.getDeliveryManagers(delivery.getDestinationHubId(), ManagerType.COMPANY_DELIVERY)
                    .stream()
                    .filter(manager -> companyManagerId.equals(manager.getUserId()))
                    .map(DeliveryManagerClientResponse::getSlackId)
                    .filter(slackId -> slackId != null && !slackId.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE));
        }

        UUID routeManagerId = routes.get(0).getDeliveryManagerId();
        if (routeManagerId == null) {
            throw new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
        }

        return userClient.getDeliveryManagers(null, ManagerType.HUB_DELIVERY)
                .stream()
                .filter(manager -> routeManagerId.equals(manager.getUserId()))
                .map(DeliveryManagerClientResponse::getSlackId)
                .filter(slackId -> slackId != null && !slackId.isBlank())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE));
    }

    private Map<UUID, String> getHubNameMap(List<UUID> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }

        List<HubSummaryClientResponse> hubs = hubClient.getHubsByIds(hubIds);
        Map<UUID, String> hubNameMap = new LinkedHashMap<>();
        for (HubSummaryClientResponse hub : hubs) {
            hubNameMap.put(hub.getHubId(), hub.getHubName());
        }

        for (UUID hubId : hubIds) {
            if (!hubNameMap.containsKey(hubId)) {
                throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
            }
        }

        return hubNameMap;
    }

    private List<UUID> buildHubNameQueryIds(UUID originHubId, List<UUID> waypointHubIds) {
        List<UUID> hubIds = new ArrayList<>();
        hubIds.add(originHubId);
        hubIds.addAll(waypointHubIds);
        return hubIds;
    }

    private List<UUID> extractWaypointHubIds(List<DeliveryRoute> routes) {
        if (routes.size() <= 1) {
            return List.of();
        }

        return routes.stream()
                .limit(routes.size() - 1L)
                .map(DeliveryRoute::getToHubId)
                .toList();
    }

    private String requireHubName(Map<UUID, String> hubNameMap, UUID hubId) {
        String hubName = hubNameMap.get(hubId);
        if (hubName == null || hubName.isBlank()) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
        return hubName;
    }

    private Integer calculateTotalDurationMin(List<DeliveryRoute> routes) {
        return routes.stream()
                .map(DeliveryRoute::getDurationMin)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private Delivery findDeliveryByRole(UUID deliveryId, UserRole userRole, UUID companyId, String username) {
        if (userRole == UserRole.COMPANY_MANAGER) {
            return deliveryRepository.findByIdAndReceiverCompanyIdAndDeletedAtIsNull(deliveryId, requireCompanyId(companyId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        }

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (userRole == UserRole.DELIVERY_MANAGER) {
            UUID userId = requireUserId(username);
            if (!userId.equals(delivery.getDeliveryManagerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return delivery;
        }

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
        UserInfoClientResponse userInfo = getRequiredUserInfo(username);
        if (userInfo.getHubId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userInfo.getHubId();
    }

    private UUID requireUserId(String username) {
        UserInfoClientResponse userInfo = getRequiredUserInfo(username);
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

    private void validateCompanyMapping(CompanyInfoClientResponse supplierCompany, CompanyInfoClientResponse receiverCompany) {
        if (supplierCompany.getHubId() == null || receiverCompany.getHubId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업체 허브 정보가 누락되어 배송 경로를 계산할 수 없습니다.");
        }

        String receiverAddress = receiverCompany.getAddress();
        if (receiverAddress == null || receiverAddress.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "수령 업체 주소가 누락되어 배송지를 생성할 수 없습니다.");
        }
    }

    private List<HubRoutePathClientResponse.HubRoutePathSegmentClientResponse> validateRoutePath(
            UUID departureHubId,
            UUID arrivalHubId,
            HubRoutePathClientResponse routePath
    ) {
        if (routePath == null || routePath.getSegments() == null) {
            throw new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        List<HubRoutePathClientResponse.HubRoutePathSegmentClientResponse> segments = routePath.getSegments();
        if (!departureHubId.equals(arrivalHubId) && segments.isEmpty()) {
            throw new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        return segments;
    }

    private DeliveryRoute toDeliveryRoute(
            UUID deliveryId,
            HubRoutePathClientResponse.HubRoutePathSegmentClientResponse segment
    ) {
        return DeliveryRoute.create(
                deliveryId,
                segment.getSequence(),
                segment.getFromHubId(),
                segment.getToHubId(),
                null,
                segment.getDistanceMeters(),
                segment.getDurationSeconds(),
                toBigDecimal(segment.getDistanceKm()),
                segment.getDurationMin()
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private void cancelLatestAssignment(UUID deliveryId) {
        deliveryAssignmentRepository
                .findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
                        deliveryId,
                        AssignmentStatus.ASSIGNED
                )
                .ifPresent(assignment -> assignment.markCancelled("배송 취소"));
    }

    private DeliveryAssignment requireActiveAssignment(UUID deliveryId) {
        return deliveryAssignmentRepository
                .findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
                        deliveryId,
                        AssignmentStatus.ASSIGNED
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));
    }
}
