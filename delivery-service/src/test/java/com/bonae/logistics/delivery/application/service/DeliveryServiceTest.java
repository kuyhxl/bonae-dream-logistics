package com.bonae.logistics.delivery.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.delivery.auth.UserRole;
import com.bonae.logistics.delivery.domain.entity.Delivery;
import com.bonae.logistics.delivery.domain.entity.DeliveryStatus;
import com.bonae.logistics.delivery.domain.repository.DeliveryRepository;
import com.bonae.logistics.delivery.infrastructure.client.CompanyClient;
import com.bonae.logistics.delivery.infrastructure.client.UserClient;
import com.bonae.logistics.delivery.infrastructure.client.dto.CompanyInfoClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import com.bonae.logistics.delivery.presentation.dto.request.DeliveryCreateRequest;
import com.bonae.logistics.delivery.presentation.dto.request.InternalDeliveryUpdateRequest;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCancelResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryCreateResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryDetailResponse;
import com.bonae.logistics.delivery.presentation.dto.response.DeliveryListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("배송 생성 시 requestNote를 저장한다")
    void createDelivery_success() throws Exception {
        DeliveryCreateRequest request = createRequest();
        CompanyInfoClientResponse supplierCompany = createCompanyInfo(UUID.randomUUID(), "공급 업체 주소");
        CompanyInfoClientResponse receiverCompany = createCompanyInfo(UUID.randomUUID(), "수령 업체 주소");
        UserInfoClientResponse receiverUser = createUser(UUID.randomUUID(), "receiver01", "홍길동", "U08ABCD1234");

        Delivery[] savedHolder = new Delivery[1];

        when(companyClient.getCompany(request.getSupplierCompanyId())).thenReturn(supplierCompany);
        when(companyClient.getCompany(request.getReceiverCompanyId())).thenReturn(receiverCompany);
        when(userClient.getUserInfo(request.getReceiverUsername())).thenReturn(receiverUser);
        when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(invocation -> {
            savedHolder[0] = invocation.getArgument(0);
            return savedHolder[0];
        });

        DeliveryCreateResponse result = deliveryService.createDelivery(request);

        assertThat(result.getDeliveryId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(result.getRouteCount()).isZero();
        assertThat(savedHolder[0].getRequestNote()).isEqualTo("12월 12일 3시까지 부탁드립니다.");
        assertThat(savedHolder[0].getReceiverName()).isEqualTo(receiverUser.getName());
        assertThat(savedHolder[0].getReceiverSlackId()).isEqualTo(receiverUser.getSlackId());
    }

    @Test
    @DisplayName("배송 단건 조회 성공")
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
    @DisplayName("배송 목록 조회 성공")
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

    @Test
    @DisplayName("배송 취소 성공")
    void cancelDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result = deliveryService.cancelDelivery(delivery.getId(), UserRole.MASTER, null);

        verify(deliveryRepository).flush();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 ID 기준 배송 취소 성공")
    void cancelDeliveryByOrderId_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result = deliveryService.cancelDeliveryByOrderId(delivery.getOrderId());

        verify(deliveryRepository).flush();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("주문 수정 연동 시 requestNote만 갱신한다")
    void updateDeliveryByOrder_success() throws Exception {
        Delivery delivery = createDelivery();
        InternalDeliveryUpdateRequest request = createInternalUpdateRequest(delivery.getOrderId(), " 12월 15일 3시까지 보내주세요! ");
        doNothing().when(deliveryRepository).flush();
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));

        DeliveryDetailResponse result = deliveryService.updateDeliveryByOrder(request);

        verify(deliveryRepository).flush();
        assertThat(delivery.getRequestNote()).isEqualTo("12월 15일 3시까지 보내주세요!");
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("배송 담당자는 배송 목록 조회 권한이 없다")
    void getDeliveries_deliveryManagerForbidden() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> deliveryService.getDeliveries(pageRequestDto, UserRole.DELIVERY_MANAGER, null, "delivery-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("허브 담당자는 자기 허브 배송만 단건 조회할 수 있다")
    void getDelivery_hubManagerScoped() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("seoul-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "seoul-manager", delivery.getOriginHubId()));

        DeliveryDetailResponse result = deliveryService.getDelivery(delivery.getId(), UserRole.HUB_MANAGER, null, "seoul-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("다른 허브 담당자는 배송 취소 권한이 없다")
    void cancelDelivery_hubManagerForbidden() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("busan-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "busan-manager", UUID.randomUUID()));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId(), UserRole.HUB_MANAGER, "busan-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("배송 완료 상태는 requestNote를 수정할 수 없다")
    void updateDeliveryByOrder_completedDeliveryForbidden() throws Exception {
        Delivery delivery = createDelivery();
        setField(delivery, "status", DeliveryStatus.DELIVERED);
        InternalDeliveryUpdateRequest request = createInternalUpdateRequest(delivery.getOrderId(), "변경 요청사항");
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateDeliveryByOrder(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    private DeliveryCreateRequest createRequest() throws Exception {
        DeliveryCreateRequest request = new DeliveryCreateRequest();
        setField(request, "orderId", UUID.randomUUID());
        setField(request, "supplierCompanyId", UUID.randomUUID());
        setField(request, "receiverCompanyId", UUID.randomUUID());
        setField(request, "receiverUsername", " receiver01 ");
        setField(request, "productInfo", " 건어물 50박스 ");
        setField(request, "requestNote", " 12월 12일 3시까지 부탁드립니다. ");
        return request;
    }

    private InternalDeliveryUpdateRequest createInternalUpdateRequest(UUID orderId, String requestNote) throws Exception {
        InternalDeliveryUpdateRequest request = new InternalDeliveryUpdateRequest();
        setField(request, "orderId", orderId);
        setField(request, "requestNote", requestNote);
        return request;
    }

    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "홍길동",
                "U123456",
                "서울시 강남구 테헤란로 1",
                "12월 12일 3시까지 부탁드립니다."
        );
    }

    private CompanyInfoClientResponse createCompanyInfo(UUID hubId, String address) {
        return CompanyInfoClientResponse.builder()
                .companyId(UUID.randomUUID())
                .hubId(hubId)
                .address(address)
                .build();
    }

    private UserInfoClientResponse createUser(UUID id, String username, String name, String slackId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .slackId(slackId)
                .role("COMPANY_MANAGER")
                .build();
    }

    private UserInfoClientResponse createHubManagerUser(UUID id, String username, UUID hubId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name("허브 담당자")
                .slackId("U01")
                .role("HUB_MANAGER")
                .hubId(hubId)
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
