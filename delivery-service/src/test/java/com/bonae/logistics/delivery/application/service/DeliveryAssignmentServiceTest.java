package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import com.bonae.logistics.delivery.domain.entity.DeliveryManager;
import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.domain.repository.DeliveryAssignmentRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryManagerRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRouteRepository;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryAssignmentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryManagerRepository deliveryManagerRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryRouteRepository deliveryRouteRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryAssignmentService deliveryAssignmentService;

    @Test
    @DisplayName("assign delivery chooses first company candidate when no previous history exists")
    void assignDelivery_firstCandidate() {
        Delivery delivery = createDelivery();
        DeliveryManager firstManager = createDeliveryManager(delivery.getDestinationHubId(), 0, ManagerType.COMPANY_DELIVERY);
        DeliveryManager secondManager = createDeliveryManager(delivery.getDestinationHubId(), 1, ManagerType.COMPANY_DELIVERY);

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId()))
                .thenReturn(List.of());
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                delivery.getDestinationHubId(),
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(firstManager, secondManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(delivery.getDestinationHubId()),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignmentResponse response =
                deliveryAssignmentService.assignDelivery(delivery.getId(), "first assignment", UserRole.MASTER, null);

        assertThat(response.getDeliveryManagerId()).isEqualTo(firstManager.getId());
        assertThat(response.getSequenceNo()).isEqualTo(1);
        assertThat(response.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(delivery.getDeliveryManagerId()).isEqualTo(firstManager.getId());
        assertThat(delivery.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("assign delivery allows same-hub out-for-delivery case")
    void assignDelivery_sameHubOutForDeliveryAllowed() throws Exception {
        UUID hubId = UUID.randomUUID();
        Delivery delivery = createDelivery(hubId, hubId);
        setField(delivery, "status", DeliveryStatus.OUT_FOR_DELIVERY);
        DeliveryManager companyManager = createDeliveryManager(hubId, 0, ManagerType.COMPANY_DELIVERY);

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId()))
                .thenReturn(List.of());
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                hubId,
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(companyManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(hubId),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignmentResponse response =
                deliveryAssignmentService.assignDelivery(delivery.getId(), "same hub assignment", UserRole.MASTER, null);

        assertThat(response.getDeliveryManagerId()).isEqualTo(companyManager.getId());
        assertThat(delivery.getDeliveryManagerId()).isEqualTo(companyManager.getId());
    }

    @Test
    @DisplayName("assign delivery also assigns hub delivery managers to routes")
    void assignDelivery_assignsHubManagersToRoutes() {
        UUID originHubId = UUID.randomUUID();
        UUID middleHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        Delivery delivery = createDelivery(originHubId, destinationHubId);
        DeliveryRoute firstRoute = createRoute(delivery.getId(), 1, originHubId, middleHubId);
        DeliveryRoute secondRoute = createRoute(delivery.getId(), 2, middleHubId, destinationHubId);
        DeliveryManager hubManager1 = createDeliveryManager(originHubId, 0, ManagerType.HUB_DELIVERY);
        DeliveryManager hubManager2 = createDeliveryManager(middleHubId, 0, ManagerType.HUB_DELIVERY);
        DeliveryManager companyManager = createDeliveryManager(destinationHubId, 0, ManagerType.COMPANY_DELIVERY);

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId()))
                .thenReturn(List.of(firstRoute, secondRoute));
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                originHubId,
                ManagerType.HUB_DELIVERY
        )).thenReturn(List.of(hubManager1));
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                middleHubId,
                ManagerType.HUB_DELIVERY
        )).thenReturn(List.of(hubManager2));
        when(deliveryRouteRepository.findRecentAssignedRouteManagerIds(eq(originHubId), any(Pageable.class)))
                .thenReturn(List.of());
        when(deliveryRouteRepository.findRecentAssignedRouteManagerIds(eq(middleHubId), any(Pageable.class)))
                .thenReturn(List.of());
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                destinationHubId,
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(companyManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(destinationHubId),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignmentResponse response =
                deliveryAssignmentService.assignDelivery(delivery.getId(), "route assignment", UserRole.MASTER, null);

        assertThat(firstRoute.getDeliveryManagerId()).isEqualTo(hubManager1.getId());
        assertThat(secondRoute.getDeliveryManagerId()).isEqualTo(hubManager2.getId());
        assertThat(response.getDeliveryManagerId()).isEqualTo(companyManager.getId());
    }

    @Test
    @DisplayName("assign delivery rotates to next company candidate after latest assignment")
    void assignDelivery_roundRobin() {
        Delivery delivery = createDelivery();
        DeliveryManager firstManager = createDeliveryManager(delivery.getDestinationHubId(), 0, ManagerType.COMPANY_DELIVERY);
        DeliveryManager secondManager = createDeliveryManager(delivery.getDestinationHubId(), 1, ManagerType.COMPANY_DELIVERY);

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoAsc(delivery.getId()))
                .thenReturn(List.of());
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                delivery.getDestinationHubId(),
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(firstManager, secondManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(delivery.getDestinationHubId()),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of(firstManager.getId()));
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignmentResponse response =
                deliveryAssignmentService.assignDelivery(delivery.getId(), null, UserRole.MASTER, null);

        assertThat(response.getDeliveryManagerId()).isEqualTo(secondManager.getId());
        assertThat(delivery.getDeliveryManagerId()).isEqualTo(secondManager.getId());
    }

    @Test
    @DisplayName("hub manager cannot assign delivery outside destination hub")
    void assignDelivery_hubManagerForbidden() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("origin-manager"))
                .thenReturn(createUserInfo(UUID.randomUUID(), "origin-manager", delivery.getOriginHubId()));

        assertThatThrownBy(() -> deliveryAssignmentService.assignDelivery(
                delivery.getId(),
                null,
                UserRole.HUB_MANAGER,
                "origin-manager"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("reassign delivery closes previous assignment and picks next company candidate")
    void reassignDelivery_success() throws Exception {
        Delivery delivery = createDelivery();
        DeliveryManager firstManager = createDeliveryManager(delivery.getDestinationHubId(), 0, ManagerType.COMPANY_DELIVERY);
        DeliveryManager secondManager = createDeliveryManager(delivery.getDestinationHubId(), 1, ManagerType.COMPANY_DELIVERY);
        setField(delivery, "deliveryManagerId", firstManager.getId());
        setField(delivery, "assignedAt", LocalDateTime.now().minusMinutes(10));

        DeliveryAssignment latestAssignment = DeliveryAssignment.create(
                delivery.getId(),
                firstManager.getId(),
                1,
                "initial assignment"
        );

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryAssignmentRepository.findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(delivery.getId()))
                .thenReturn(Optional.of(latestAssignment));
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                delivery.getDestinationHubId(),
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(firstManager, secondManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(delivery.getDestinationHubId()),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of(firstManager.getId()));
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignmentResponse response =
                deliveryAssignmentService.reassignDelivery(delivery.getId(), "reassign", UserRole.MASTER, null);

        assertThat(response.getDeliveryManagerId()).isEqualTo(secondManager.getId());
        assertThat(response.getSequenceNo()).isEqualTo(2);
        assertThat(delivery.getDeliveryManagerId()).isEqualTo(secondManager.getId());
        assertThat(latestAssignment.getAssignmentStatus()).isEqualTo(AssignmentStatus.REASSIGNED);
        assertThat(latestAssignment.getUnassignedAt()).isNotNull();
    }

    @Test
    @DisplayName("reassign delivery fails when no alternative company manager exists")
    void reassignDelivery_noAlternativeManager() throws Exception {
        Delivery delivery = createDelivery();
        DeliveryManager onlyManager = createDeliveryManager(delivery.getDestinationHubId(), 0, ManagerType.COMPANY_DELIVERY);
        setField(delivery, "deliveryManagerId", onlyManager.getId());

        DeliveryAssignment latestAssignment = DeliveryAssignment.create(
                delivery.getId(),
                onlyManager.getId(),
                1,
                "initial assignment"
        );

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryAssignmentRepository.findTopByDeliveryIdAndDeletedAtIsNullOrderBySequenceNoDesc(delivery.getId()))
                .thenReturn(Optional.of(latestAssignment));
        when(deliveryManagerRepository.findAllByHubIdAndManagerTypeAndDeletedAtIsNullOrderByDeliverySequenceAsc(
                delivery.getDestinationHubId(),
                ManagerType.COMPANY_DELIVERY
        )).thenReturn(List.of(onlyManager));
        when(deliveryAssignmentRepository.findRecentAssignedManagerIds(
                eq(delivery.getDestinationHubId()),
                eq(ManagerType.COMPANY_DELIVERY),
                any(Pageable.class)
        )).thenReturn(List.of(onlyManager.getId()));

        assertThatThrownBy(() -> deliveryAssignmentService.reassignDelivery(
                delivery.getId(),
                "reassign",
                UserRole.MASTER,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE);
    }

    private Delivery createDelivery() {
        return createDelivery(UUID.randomUUID(), UUID.randomUUID());
    }

    private Delivery createDelivery(UUID originHubId, UUID destinationHubId) {
        return Delivery.create(
                UUID.randomUUID(),
                originHubId,
                destinationHubId,
                UUID.randomUUID(),
                "Receiver",
                "U123456",
                "Seoul Address",
                "Deliver before 3 PM"
        );
    }

    private DeliveryRoute createRoute(UUID deliveryId, int sequenceNo, UUID fromHubId, UUID toHubId) {
        return DeliveryRoute.create(
                deliveryId,
                sequenceNo,
                fromHubId,
                toHubId,
                null,
                1000,
                600,
                BigDecimal.ONE,
                10
        );
    }

    private DeliveryManager createDeliveryManager(UUID hubId, int sequence, ManagerType managerType) {
        return DeliveryManager.create(
                UUID.randomUUID(),
                hubId,
                managerType,
                sequence
        );
    }

    private UserInfoClientResponse createUserInfo(UUID id, String username, UUID hubId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .hubId(hubId)
                .role("HUB_MANAGER")
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
