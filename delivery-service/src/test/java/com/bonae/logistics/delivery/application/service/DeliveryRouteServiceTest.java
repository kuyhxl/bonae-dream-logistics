package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRouteRepository;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryRouteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRouteRepository deliveryRouteRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryRouteService deliveryRouteService;

    @Test
    @DisplayName("master can read all routes")
    void getDeliveryRoutes_master() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .thenReturn(Optional.of(createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null)));
        DeliveryRoute firstRoute = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        DeliveryRoute secondRoute = createRoute(deliveryId, 2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
                .thenReturn(List.of(firstRoute, secondRoute));

        List<DeliveryRouteResponse> result =
                deliveryRouteService.getDeliveryRoutes(deliveryId, UserRole.MASTER, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSequenceNo()).isEqualTo(1);
        assertThat(result.get(1).getSequenceNo()).isEqualTo(2);
    }

    @Test
    @DisplayName("hub manager can read all routes of accessible delivery")
    void getDeliveryRoutes_hubManagerCanReadAllRoutesOfAccessibleDelivery() {
        UUID deliveryId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .thenReturn(Optional.of(createDelivery(deliveryId, hubId, UUID.randomUUID(), null)));
        DeliveryRoute firstRoute = createRoute(deliveryId, 1, hubId, UUID.randomUUID(), UUID.randomUUID());
        DeliveryRoute secondRoute = createRoute(deliveryId, 2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
                .thenReturn(List.of(firstRoute, secondRoute));
        when(userClient.getUserInfo("hub-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "hub-manager", hubId));

        List<DeliveryRouteResponse> result =
                deliveryRouteService.getDeliveryRoutes(deliveryId, UserRole.HUB_MANAGER, "hub-manager");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRouteId()).isEqualTo(firstRoute.getId());
        assertThat(result.get(1).getRouteId()).isEqualTo(secondRoute.getId());
    }

    @Test
    @DisplayName("delivery manager can read all routes of assigned delivery")
    void getDeliveryRoutes_deliveryManagerCanReadAllRoutesOfAssignedDelivery() {
        UUID deliveryId = UUID.randomUUID();
        UUID deliveryManagerId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .thenReturn(Optional.of(createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), deliveryManagerId)));
        DeliveryRoute firstRoute = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), deliveryManagerId);
        DeliveryRoute secondRoute = createRoute(deliveryId, 2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
                .thenReturn(List.of(firstRoute, secondRoute));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createDeliveryManagerUser(deliveryManagerId, "delivery-manager"));

        List<DeliveryRouteResponse> result =
                deliveryRouteService.getDeliveryRoutes(deliveryId, UserRole.DELIVERY_MANAGER, "delivery-manager");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRouteId()).isEqualTo(firstRoute.getId());
        assertThat(result.get(1).getRouteId()).isEqualTo(secondRoute.getId());
    }

    @Test
    @DisplayName("delivery manager without assignment cannot read routes")
    void getDeliveryRoutes_deliveryManagerForbidden() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .thenReturn(Optional.of(createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createDeliveryManagerUser(UUID.randomUUID(), "delivery-manager"));

        assertThatThrownBy(() -> deliveryRouteService.getDeliveryRoutes(deliveryId, UserRole.DELIVERY_MANAGER, "delivery-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("route status can move from waiting to in transit")
    void updateRouteStatus_depart() {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Delivery delivery = createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null);
        trySetStatus(delivery, DeliveryStatus.HUB_WAITING);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));

        DeliveryRouteResponse result =
                deliveryRouteService.updateRouteStatus(route.getId(), RouteStatus.IN_TRANSIT, UserRole.MASTER, null);

        assertThat(result.getRouteStatus()).isEqualTo(RouteStatus.IN_TRANSIT);
        assertThat(result.getActualDepartedAt()).isNotNull();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    @DisplayName("last route arrival moves delivery to out for delivery")
    void updateRouteStatus_arrive() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Delivery delivery = createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null);
        setField(route, "routeStatus", RouteStatus.IN_TRANSIT);
        setField(route, "actualDepartedAt", LocalDateTime.now().minusMinutes(15));
        setField(delivery, "status", DeliveryStatus.HUB_MOVING);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(deliveryId))
                .thenReturn(List.of(route));

        DeliveryRouteResponse result =
                deliveryRouteService.updateRouteStatus(route.getId(), RouteStatus.ARRIVED, UserRole.MASTER, null);

        assertThat(result.getRouteStatus()).isEqualTo(RouteStatus.ARRIVED);
        assertThat(result.getActualArrivedAt()).isNotNull();
        assertThat(result.getActualDurationMin()).isNotNull();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);
    }

    @Test
    @DisplayName("cannot depart next route before previous route arrives")
    void updateRouteStatus_departNextRouteBeforePreviousArrivedForbidden() {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Delivery delivery = createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null);
        trySetStatus(delivery, DeliveryStatus.HUB_WAITING);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.existsByDeliveryIdAndSequenceNoAndRouteStatusAndDeletedAtIsNull(
                deliveryId,
                1,
                RouteStatus.ARRIVED
        )).thenReturn(false);

        assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(
                route.getId(),
                RouteStatus.IN_TRANSIT,
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("hub manager cannot update unrelated route")
    void updateRouteStatus_hubManagerForbidden() {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(userClient.getUserInfo("hub-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "hub-manager", UUID.randomUUID()));

        assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(
                route.getId(),
                RouteStatus.IN_TRANSIT,
                UserRole.HUB_MANAGER,
                "hub-manager"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("invalid route transition throws exception")
    void updateRouteStatus_invalidTransition() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        setField(route, "routeStatus", RouteStatus.ARRIVED);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId))
                .thenReturn(Optional.of(createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null)));

        assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(
                route.getId(),
                RouteStatus.IN_TRANSIT,
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("cancelled parent delivery blocks route transition")
    void updateRouteStatus_cancelledDeliveryForbidden() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Delivery delivery = createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null);
        setField(delivery, "status", DeliveryStatus.CANCELLED);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(
                route.getId(),
                RouteStatus.IN_TRANSIT,
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("delivered parent delivery blocks route transition")
    void updateRouteStatus_deliveredDeliveryForbidden() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryRoute route = createRoute(deliveryId, 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Delivery delivery = createDelivery(deliveryId, UUID.randomUUID(), UUID.randomUUID(), null);
        setField(delivery, "status", DeliveryStatus.DELIVERED);
        when(deliveryRouteRepository.findByIdAndDeletedAtIsNull(route.getId())).thenReturn(Optional.of(route));
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryRouteService.updateRouteStatus(
                route.getId(),
                RouteStatus.IN_TRANSIT,
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_ALREADY_COMPLETED);
    }

    private DeliveryRoute createRoute(
            UUID deliveryId,
            Integer sequenceNo,
            UUID fromHubId,
            UUID toHubId,
            UUID deliveryManagerId
    ) {
        return DeliveryRoute.create(
                deliveryId,
                sequenceNo,
                fromHubId,
                toHubId,
                deliveryManagerId,
                1000,
                600,
                BigDecimal.valueOf(1.0),
                10
        );
    }

    private Delivery createDelivery(UUID orderId, UUID originHubId, UUID destinationHubId, UUID deliveryManagerId) {
        Delivery delivery = Delivery.create(
                orderId,
                originHubId,
                destinationHubId,
                UUID.randomUUID(),
                "Receiver",
                "U123456",
                "Seoul Address",
                "Deliver before 3 PM"
        );
        if (deliveryManagerId != null) {
            try {
                setField(delivery, "deliveryManagerId", deliveryManagerId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return delivery;
    }

    private UserInfoClientResponse createHubManagerUser(UUID id, String username, UUID hubId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .hubId(hubId)
                .role("HUB_MANAGER")
                .build();
    }

    private UserInfoClientResponse createDeliveryManagerUser(UUID id, String username) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .role("DELIVERY_MANAGER")
                .build();
    }

    private void trySetStatus(Delivery delivery, DeliveryStatus status) {
        try {
            setField(delivery, "status", status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
