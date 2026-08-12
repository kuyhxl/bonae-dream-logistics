package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryAssignment;
import com.bonae.logistics.delivery.domain.entity.DeliveryRoute;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.entity.AssignmentStatus;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryAssignmentRepository;
import com.bonae.logistics.delivery.domain.repository.DeliveryRouteRepository;
import com.bonae.logistics.delivery.infrastructure.client.CompanyClient;
import com.bonae.logistics.delivery.infrastructure.client.HubRouteClient;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.CompanyInfoClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.HubRoutePathClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.InternalDeliveryUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCompleteResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRouteRepository deliveryRouteRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private HubRouteClient hubRouteClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("create delivery stores request note and routes")
    void createDelivery_success() {
        DeliveryCreateRequest request = createRequest();
        CompanyInfoClientResponse supplierCompany = createCompanyInfo(
                UUID.randomUUID(),
                "Supplier Company",
                "SUPPLIER",
                "Seoul Address"
        );
        CompanyInfoClientResponse receiverCompany = createCompanyInfo(
                UUID.randomUUID(),
                "Receiver Company",
                "RECEIVER",
                "Busan Address"
        );
        UserInfoClientResponse receiverUser = createUserInfo(
                UUID.randomUUID(),
                "receiver01",
                "Receiver User",
                "U08ABCD1234",
                "COMPANY_MANAGER",
                null,
                receiverCompany.getCompanyId()
        );
        HubRoutePathClientResponse routePath = createRoutePath(
                supplierCompany.getHubId(),
                receiverCompany.getHubId()
        );

        when(companyClient.getCompany(request.getSupplierCompanyId())).thenReturn(supplierCompany);
        when(companyClient.getCompany(request.getReceiverCompanyId())).thenReturn(receiverCompany);
        when(userClient.getUserInfo(request.getReceiverUsername())).thenReturn(receiverUser);
        when(hubRouteClient.getShortestPath(supplierCompany.getHubId(), receiverCompany.getHubId())).thenReturn(routePath);
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRouteRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryCreateResponse result = deliveryService.createDelivery(request);

        assertThat(result.getDeliveryId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);
        assertThat(result.getDepartureHubId()).isEqualTo(supplierCompany.getHubId());
        assertThat(result.getArrivalHubId()).isEqualTo(receiverCompany.getHubId());
        assertThat(result.getRouteCount()).isEqualTo(2);

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture());
        Delivery savedDelivery = deliveryCaptor.getValue();
        assertThat(savedDelivery.getRequestNote()).isEqualTo("Deliver before 3 PM");
        assertThat(savedDelivery.getReceiverName()).isEqualTo("Receiver User");
        assertThat(savedDelivery.getReceiverSlackId()).isEqualTo("U08ABCD1234");
        assertThat(savedDelivery.getDeliveryAddress()).isEqualTo("Busan Address");
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.HUB_WAITING);

        ArgumentCaptor<List<DeliveryRoute>> routeCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryRouteRepository).saveAll(routeCaptor.capture());
        assertThat(routeCaptor.getValue()).hasSize(2);
        assertThat(routeCaptor.getValue())
                .extracting(DeliveryRoute::getSequenceNo)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("create delivery skips hub movement when origin and destination hubs are same")
    void createDelivery_sameHubSkipsRoutes() {
        DeliveryCreateRequest request = createRequest();
        UUID sameHubId = UUID.randomUUID();
        CompanyInfoClientResponse supplierCompany = createCompanyInfo(
                sameHubId,
                "Supplier Company",
                "SUPPLIER",
                "Seoul Address"
        );
        CompanyInfoClientResponse receiverCompany = createCompanyInfo(
                sameHubId,
                "Receiver Company",
                "RECEIVER",
                "Seoul Address"
        );
        UserInfoClientResponse receiverUser = createUserInfo(
                UUID.randomUUID(),
                "receiver01",
                "Receiver User",
                "U08ABCD1234",
                "COMPANY_MANAGER",
                null,
                receiverCompany.getCompanyId()
        );
        HubRoutePathClientResponse routePath = HubRoutePathClientResponse.builder()
                .totalDistanceMeters(0L)
                .totalDurationSeconds(0L)
                .totalDistanceKm(0.0)
                .totalDurationMin(0L)
                .segments(List.of())
                .build();

        when(companyClient.getCompany(request.getSupplierCompanyId())).thenReturn(supplierCompany);
        when(companyClient.getCompany(request.getReceiverCompanyId())).thenReturn(receiverCompany);
        when(userClient.getUserInfo(request.getReceiverUsername())).thenReturn(receiverUser);
        when(hubRouteClient.getShortestPath(sameHubId, sameHubId)).thenReturn(routePath);
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRouteRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryCreateResponse result = deliveryService.createDelivery(request);

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);
        assertThat(result.getRouteCount()).isZero();

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getStatus()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);

        ArgumentCaptor<List<DeliveryRoute>> routeCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryRouteRepository).saveAll(routeCaptor.capture());
        assertThat(routeCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("get delivery returns detail response")
    void getDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.getDelivery(delivery.getId(), UserRole.MASTER, null, null);

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getOrderId()).isEqualTo(delivery.getOrderId());
        assertThat(result.getRequestNote()).isEqualTo(delivery.getRequestNote());
        assertThat(result.getStatus()).isEqualTo(delivery.getStatus());
    }

    @Test
    @DisplayName("get deliveries rejects delivery manager")
    void getDeliveries_deliveryManagerForbidden() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> deliveryService.getDeliveries(pageRequestDto, UserRole.DELIVERY_MANAGER, null, "delivery-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("company manager can read own delivery")
    void getDelivery_companyManagerScoped() {
        Delivery delivery = createDelivery();
        UUID companyId = delivery.getReceiverCompanyId();
        when(deliveryRepository.findByIdAndReceiverCompanyIdAndDeletedAtIsNull(delivery.getId(), companyId))
                .thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result =
                deliveryService.getDelivery(delivery.getId(), UserRole.COMPANY_MANAGER, companyId, null);

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("hub manager can read delivery in own hub scope")
    void getDelivery_hubManagerScoped() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("seoul-manager"))
                .thenReturn(createUserInfo(
                        UUID.randomUUID(),
                        "seoul-manager",
                        "Seoul Manager",
                        "U01",
                        "HUB_MANAGER",
                        delivery.getOriginHubId(),
                        null
                ));

        DeliveryDetailResponse result =
                deliveryService.getDelivery(delivery.getId(), UserRole.HUB_MANAGER, null, "seoul-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("delivery manager can read assigned delivery")
    void getDelivery_deliveryManagerOwnDelivery() throws Exception {
        Delivery delivery = createDelivery();
        UUID deliveryManagerId = UUID.randomUUID();
        setField(delivery, "deliveryManagerId", deliveryManagerId);
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createUserInfo(
                        deliveryManagerId,
                        "delivery-manager",
                        "Delivery Manager",
                        "U03",
                        "DELIVERY_MANAGER",
                        null,
                        null
                ));

        DeliveryDetailResponse result =
                deliveryService.getDelivery(delivery.getId(), UserRole.DELIVERY_MANAGER, null, "delivery-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("cancel delivery by order id updates status")
    void cancelDeliveryByOrderId_success() {
        Delivery delivery = createDelivery();
        DeliveryAssignment assignment = DeliveryAssignment.create(
                delivery.getId(),
                UUID.randomUUID(),
                1,
                "초기 배정"
        );
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));
        when(deliveryAssignmentRepository.findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
                delivery.getId(),
                AssignmentStatus.ASSIGNED
        )).thenReturn(Optional.of(assignment));

        DeliveryCancelResponse result = deliveryService.cancelDeliveryByOrderId(delivery.getOrderId());

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(assignment.getAssignmentStatus()).isEqualTo(AssignmentStatus.CANCELLED);
        assertThat(assignment.getUnassignedAt()).isNotNull();
        verify(deliveryRepository).flush();
    }

    @Test
    @DisplayName("delivery manager can complete assigned delivery and close assignment")
    void completeDelivery_success() throws Exception {
        Delivery delivery = createDelivery();
        UUID deliveryManagerId = UUID.randomUUID();
        setField(delivery, "deliveryManagerId", deliveryManagerId);
        setField(delivery, "status", DeliveryStatus.OUT_FOR_DELIVERY);
        DeliveryAssignment assignment = DeliveryAssignment.create(
                delivery.getId(),
                deliveryManagerId,
                1,
                "초기 배정"
        );

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createUserInfo(
                        deliveryManagerId,
                        "delivery-manager",
                        "Delivery Manager",
                        "U03",
                        "DELIVERY_MANAGER",
                        null,
                        null
                ));
        when(deliveryAssignmentRepository.findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
                delivery.getId(),
                AssignmentStatus.ASSIGNED
        )).thenReturn(Optional.of(assignment));

        DeliveryCompleteResponse result =
                deliveryService.completeDelivery(delivery.getId(), UserRole.DELIVERY_MANAGER, "delivery-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(assignment.getAssignmentStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(assignment.getUnassignedAt()).isNotNull();
        verify(deliveryRepository).flush();
    }

    @Test
    @DisplayName("cannot complete delivery outside out for delivery status")
    void completeDelivery_invalidStatus() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.completeDelivery(delivery.getId(), UserRole.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("cannot complete delivery without active assignment")
    void completeDelivery_withoutActiveAssignment() throws Exception {
        Delivery delivery = createDelivery();
        UUID deliveryManagerId = UUID.randomUUID();
        setField(delivery, "deliveryManagerId", deliveryManagerId);
        setField(delivery, "status", DeliveryStatus.OUT_FOR_DELIVERY);

        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createUserInfo(
                        deliveryManagerId,
                        "delivery-manager",
                        "Delivery Manager",
                        "U03",
                        "DELIVERY_MANAGER",
                        null,
                        null
                ));
        when(deliveryAssignmentRepository.findTopByDeliveryIdAndAssignmentStatusAndDeletedAtIsNullOrderBySequenceNoDesc(
                delivery.getId(),
                AssignmentStatus.ASSIGNED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.completeDelivery(
                delivery.getId(),
                UserRole.DELIVERY_MANAGER,
                "delivery-manager"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("update delivery by order changes request note only")
    void updateDeliveryByOrder_success() {
        Delivery delivery = createDelivery();
        InternalDeliveryUpdateRequest request = createInternalUpdateRequest(delivery.getOrderId(), "Updated note");
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.updateDeliveryByOrder(request);

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getRequestNote()).isEqualTo("Updated note");
        assertThat(delivery.getRequestNote()).isEqualTo("Updated note");
        verify(deliveryRepository).flush();
    }

    @Test
    @DisplayName("master delivery list query returns content")
    void getDeliveries_success() {
        Delivery delivery = createDelivery();
        PageRequestDto pageRequestDto = new PageRequestDto();
        when(deliveryRepository.findAllByDeletedAtIsNull(any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        PageResponseDto<DeliveryListItemResponse> result =
                deliveryService.getDeliveries(pageRequestDto, UserRole.MASTER, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
    }

    private DeliveryCreateRequest createRequest() {
        DeliveryCreateRequest request = new DeliveryCreateRequest();
        try {
            setField(request, "orderId", UUID.randomUUID());
            setField(request, "supplierCompanyId", UUID.randomUUID());
            setField(request, "receiverCompanyId", UUID.randomUUID());
            setField(request, "receiverUsername", "receiver01");
            setField(request, "productInfo", "Dried squid 50 boxes");
            setField(request, "requestNote", "Deliver before 3 PM");
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InternalDeliveryUpdateRequest createInternalUpdateRequest(UUID orderId, String requestNote) {
        InternalDeliveryUpdateRequest request = new InternalDeliveryUpdateRequest();
        try {
            setField(request, "orderId", orderId);
            setField(request, "requestNote", requestNote);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HubRoutePathClientResponse createRoutePath(UUID departureHubId, UUID arrivalHubId) {
        return HubRoutePathClientResponse.builder()
                .totalDistanceMeters(3000L)
                .totalDurationSeconds(1800L)
                .totalDistanceKm(3.0)
                .totalDurationMin(30L)
                .segments(List.of(
                        HubRoutePathClientResponse.HubRoutePathSegmentClientResponse.builder()
                                .sequence(1)
                                .fromHubId(departureHubId)
                                .toHubId(UUID.randomUUID())
                                .distanceMeters(1000)
                                .durationSeconds(600)
                                .distanceKm(1.0)
                                .durationMin(10)
                                .build(),
                        HubRoutePathClientResponse.HubRoutePathSegmentClientResponse.builder()
                                .sequence(2)
                                .fromHubId(UUID.randomUUID())
                                .toHubId(arrivalHubId)
                                .distanceMeters(2000)
                                .durationSeconds(1200)
                                .distanceKm(2.0)
                                .durationMin(20)
                                .build()
                ))
                .build();
    }

    private CompanyInfoClientResponse createCompanyInfo(
            UUID hubId,
            String name,
            String type,
            String address
    ) {
        return CompanyInfoClientResponse.builder()
                .companyId(UUID.randomUUID())
                .name(name)
                .type(type)
                .hubId(hubId)
                .address(address)
                .isDeleted(false)
                .build();
    }

    private UserInfoClientResponse createUserInfo(
            UUID id,
            String username,
            String name,
            String slackId,
            String role,
            UUID hubId,
            UUID companyId
    ) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .slackId(slackId)
                .role(role)
                .hubId(hubId)
                .companyId(companyId)
                .build();
    }

    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Receiver",
                "U123456",
                "Seoul Address",
                "Deliver before 3 PM"
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
