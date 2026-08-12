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
import org.mockito.ArgumentCaptor;
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
    @DisplayName("배송 생성 시 요청사항을 저장한다")
    void createDelivery_success() throws Exception {
        DeliveryCreateRequest request = createRequest();
        when(companyClient.getCompany(request.getSupplierCompanyId()))
                .thenReturn(createCompanyInfo(UUID.randomUUID(), "공급 업체 주소"));
        when(companyClient.getCompany(request.getReceiverCompanyId()))
                .thenReturn(createCompanyInfo(UUID.randomUUID(), "수령 업체 주소"));
        when(userClient.getUserInfo(request.getReceiverUsername()))
                .thenReturn(createCompanyManagerUser(UUID.randomUUID(), "receiver01", "홍길동", "U08ABCD1234"));
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryCreateResponse result = deliveryService.createDelivery(request);

        assertThat(result.getDeliveryId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(result.getRouteCount()).isZero();

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getRequestNote())
                .isEqualTo("12월 12일 3시까지 부탁드립니다.");
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
    @DisplayName("존재하지 않는 배송 조회 시 예외 발생")
    void getDelivery_notFound() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, UserRole.MASTER, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_NOT_FOUND);
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
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(delivery.getStatus());
    }

    @Test
    @DisplayName("업체 담당자는 자기 업체 배송만 단건 조회")
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
    @DisplayName("허브 담당자는 자기 허브 배송만 단건 조회")
    void getDelivery_hubManagerScoped() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("seoul-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "seoul-manager", "서울담당자", "U01",
                        delivery.getOriginHubId(), null));

        DeliveryDetailResponse result =
                deliveryService.getDelivery(delivery.getId(), UserRole.HUB_MANAGER, null, "seoul-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("허브 담당자는 타 허브 배송 단건 조회 불가")
    void getDelivery_hubManagerForbidden() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("busan-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "busan-manager", "부산담당자", "U02",
                        UUID.randomUUID(), null));

        assertThatThrownBy(() -> deliveryService.getDelivery(delivery.getId(), UserRole.HUB_MANAGER, null, "busan-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("배송 담당자는 본인 배정 배송 단건 조회 가능")
    void getDelivery_deliveryManagerOwnDelivery() throws Exception {
        Delivery delivery = createDelivery();
        UUID deliveryManagerId = UUID.randomUUID();
        setField(delivery, "deliveryManagerId", deliveryManagerId);
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createDeliveryManagerUser(deliveryManagerId, "delivery-manager", "배송담당자", "U03"));

        DeliveryDetailResponse result =
                deliveryService.getDelivery(delivery.getId(), UserRole.DELIVERY_MANAGER, null, "delivery-manager");

        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("배송 담당자는 타인 배정 배송 단건 조회 불가")
    void getDelivery_deliveryManagerForbidden() throws Exception {
        Delivery delivery = createDelivery();
        setField(delivery, "deliveryManagerId", UUID.randomUUID());
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("delivery-manager"))
                .thenReturn(createDeliveryManagerUser(UUID.randomUUID(), "delivery-manager", "배송담당자", "U03"));

        assertThatThrownBy(() -> deliveryService.getDelivery(delivery.getId(), UserRole.DELIVERY_MANAGER, null, "delivery-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("업체 담당자는 companyId 헤더 없으면 목록 조회 불가")
    void getDeliveries_companyManagerWithoutCompanyId() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> deliveryService.getDeliveries(pageRequestDto, UserRole.COMPANY_MANAGER, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("업체 담당자는 자기 업체 배송만 목록 조회")
    void getDeliveries_companyManagerScoped() {
        Delivery delivery = createDelivery();
        PageRequestDto pageRequestDto = new PageRequestDto();
        UUID companyId = delivery.getReceiverCompanyId();
        when(deliveryRepository.findAllByReceiverCompanyIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        PageResponseDto<DeliveryListItemResponse> result =
                deliveryService.getDeliveries(pageRequestDto, UserRole.COMPANY_MANAGER, companyId, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("허브 담당자는 자기 허브 기준으로 목록 조회")
    void getDeliveries_hubManagerScoped() {
        Delivery delivery = createDelivery();
        PageRequestDto pageRequestDto = new PageRequestDto();
        UUID hubId = delivery.getOriginHubId();
        when(userClient.getUserInfo("seoul-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "seoul-manager", "서울담당자", "U01", hubId, null));
        when(deliveryRepository.findAllByHubIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(new PageImpl<>(List.of(delivery)));

        PageResponseDto<DeliveryListItemResponse> result =
                deliveryService.getDeliveries(pageRequestDto, UserRole.HUB_MANAGER, null, "seoul-manager");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryId()).isEqualTo(delivery.getId());
    }

    @Test
    @DisplayName("배송 담당자는 배송 목록 조회 권한 없음")
    void getDeliveries_deliveryManagerForbidden() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> deliveryService.getDeliveries(pageRequestDto, UserRole.DELIVERY_MANAGER, null, "delivery-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("배송 취소 성공")
    void cancelDelivery_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result = deliveryService.cancelDelivery(delivery.getId(), UserRole.MASTER, null);

        verify(deliveryRepository).flush();
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("배송 완료 상태는 취소 불가")
    void cancelDelivery_completed() throws Exception {
        Delivery delivery = createDelivery();
        setField(delivery, "status", DeliveryStatus.DELIVERED);
        setField(delivery, "completedAt", LocalDateTime.now());
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId(), UserRole.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("이동 중 배송은 취소 불가")
    void cancelDelivery_movingForbidden() throws Exception {
        Delivery delivery = createDelivery();
        setField(delivery, "status", DeliveryStatus.HUB_MOVING);
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId(), UserRole.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("허브 담당자는 자기 허브 배송만 취소 가능")
    void cancelDelivery_hubManagerScoped() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("seoul-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "seoul-manager", "서울담당자", "U01",
                        delivery.getOriginHubId(), null));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result =
                deliveryService.cancelDelivery(delivery.getId(), UserRole.HUB_MANAGER, "seoul-manager");

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("허브 담당자는 타 허브 배송 취소 불가")
    void cancelDelivery_hubManagerForbidden() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByIdAndDeletedAtIsNull(delivery.getId())).thenReturn(Optional.of(delivery));
        when(userClient.getUserInfo("busan-manager"))
                .thenReturn(createHubManagerUser(UUID.randomUUID(), "busan-manager", "부산담당자", "U02",
                        UUID.randomUUID(), null));

        assertThatThrownBy(() -> deliveryService.cancelDelivery(delivery.getId(), UserRole.HUB_MANAGER, "busan-manager"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("주문 ID 기준 배송 취소 성공")
    void cancelDeliveryByOrderId_success() {
        Delivery delivery = createDelivery();
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryCancelResponse result = deliveryService.cancelDeliveryByOrderId(delivery.getOrderId());

        verify(deliveryRepository).flush();
        assertThat(result.getDeliveryId()).isEqualTo(delivery.getId());
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 ID 기준 배송이 없으면 예외 발생")
    void cancelDeliveryByOrderId_notFound() {
        UUID orderId = UUID.randomUUID();
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.cancelDeliveryByOrderId(orderId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 수정 연동 시 배송 요청사항을 갱신한다")
    void updateDeliveryByOrder_success() throws Exception {
        Delivery delivery = createDelivery();
        InternalDeliveryUpdateRequest request =
                createInternalUpdateRequest(delivery.getOrderId(), " 12월 15일 3시까지 보내주세요! ");

        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(delivery.getOrderId())).thenReturn(Optional.of(delivery));
        doNothing().when(deliveryRepository).flush();

        DeliveryDetailResponse result = deliveryService.updateDeliveryByOrder(request);

        verify(deliveryRepository).flush();
        assertThat(result.getOrderId()).isEqualTo(delivery.getOrderId());
        assertThat(delivery.getRequestNote()).isEqualTo("12월 15일 3시까지 보내주세요!");
        assertThat(result.getRequestNote()).isEqualTo(delivery.getRequestNote());
    }

    @Test
    @DisplayName("주문 수정 기준 배송 수정 시 배송이 없으면 예외 발생")
    void updateDeliveryByOrder_notFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        InternalDeliveryUpdateRequest request = createInternalUpdateRequest(orderId, "변경 요청사항");
        when(deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.updateDeliveryByOrder(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELIVERY_NOT_FOUND);
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
                "hong123",
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

    private UserInfoClientResponse createCompanyManagerUser(UUID id, String username, String name, String slackId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .slackId(slackId)
                .role("COMPANY_MANAGER")
                .build();
    }

    private UserInfoClientResponse createHubManagerUser(
            UUID id,
            String username,
            String name,
            String slackId,
            UUID hubId,
            UUID companyId
    ) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .slackId(slackId)
                .role("HUB_MANAGER")
                .hubId(hubId)
                .companyId(companyId)
                .build();
    }

    private UserInfoClientResponse createDeliveryManagerUser(UUID id, String username, String name, String slackId) {
        return UserInfoClientResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .slackId(slackId)
                .role("DELIVERY_MANAGER")
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
