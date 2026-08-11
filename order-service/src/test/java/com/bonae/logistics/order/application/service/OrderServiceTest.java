package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.domain.entity.Order;
import com.bonae.logistics.order.domain.entity.OrderStatus;
import com.bonae.logistics.order.domain.repository.OrderRepository;
import com.bonae.logistics.order.infrastructure.client.DeliveryClient;
import com.bonae.logistics.order.infrastructure.client.InventoryClient;
import com.bonae.logistics.order.infrastructure.client.ProductClient;
import com.bonae.logistics.order.infrastructure.client.SlackClient;
import com.bonae.logistics.order.infrastructure.client.dto.response.DeliveryCreateResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryDeductResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryRestoreResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.ProductInfoResponseDto;
import com.bonae.logistics.order.infrastructure.config.AlertProperties;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductClient productClient;
    @Mock private InventoryClient inventoryClient;
    @Mock private DeliveryClient deliveryClient;
    @Mock private SlackClient slackClient;
    @Mock private AlertProperties alertProperties;

    @InjectMocks private OrderService orderService;

    private static final UUID REQUESTER_COMPANY_ID = UUID.randomUUID();
    private static final UUID RECEIVER_COMPANY_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_COMPANY_ID = UUID.randomUUID();
    private static final UUID DELIVERY_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();

    private OrderCreateRequestDto requestDto;
    private ProductInfoResponseDto productInfoResponseDto;

    @BeforeEach
    void setUp() {
        requestDto = new OrderCreateRequestDto(
                RECEIVER_COMPANY_ID,
                PRODUCT_ID,
                10,
                LocalDateTime.now().plusDays(3),
                "빨리 보내주세요"
        );

        productInfoResponseDto = new ProductInfoResponseDto(
                PRODUCT_ID, "마른오징어", SUPPLIER_COMPANY_ID, BigDecimal.valueOf(10000), false
        );
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        private static final String USER_ID = "hong_gildong";

        @Test
        @DisplayName("주문 생성 성공")
        void create_success() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderResponseDto response = orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("PENDING");

            verify(productClient).getProductInfo(PRODUCT_ID);
            verify(inventoryClient).deductStock(eq(PRODUCT_ID), any());
            verify(deliveryClient).createDelivery(any());
            verify(orderRepository).save(any(Order.class));
            verifyNoInteractions(slackClient);
        }

        @Test
        @DisplayName("주문 생성 실패 - 삭제된 상품일 경우")
        void create_fail_productDeleted() {
            // given
            ProductInfoResponseDto deletedProduct = new ProductInfoResponseDto(
                    PRODUCT_ID, "삭제된상품", SUPPLIER_COMPANY_ID, BigDecimal.TEN, true
            );
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(deletedProduct);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
            verifyNoInteractions(inventoryClient, deliveryClient, orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 재고 부족일 경우")
        void create_fail_stockShortage() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE));

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STOCK_SHORTAGE);
            verifyNoInteractions(deliveryClient, orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 배송 생성 실패 시 재고 복원(보상 트랜잭션) 수행")
        void create_fail_deliveryFailed_thenRestoreStock() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
            given(inventoryClient.restoreStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryRestoreResponseDto(PRODUCT_ID, 100));

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
            verify(inventoryClient, times(1)).restoreStock(eq(PRODUCT_ID), any());
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 배송 생성 및 재고 복원이 모두 실패하면 원래 에러가 전파된다")
        void create_fail_deliveryAndRestoreBothFailed() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE));
            given(inventoryClient.restoreStock(eq(PRODUCT_ID), any()))
                    .willThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("주문 생성 성공 - 요청업체(requesterCompanyId)가 없는 경우도 허용")
        void create_success_withoutRequesterCompanyId() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderResponseDto response = orderService.createOrder(requestDto, null, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("배송 생성 요청 시 supplierCompanyId는 상품의 companyId를 사용한다")
        void create_deliveryRequest_usesProductCompanyIdAsSupplier() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            verify(deliveryClient).createDelivery(argThat(request ->
                    request.supplierCompanyId().equals(SUPPLIER_COMPANY_ID)
            ));
        }

        @Test
        @DisplayName("배송 생성 요청 시 receiverUsername은 요청자의 userId를 사용한다")
        void create_deliveryRequest_usesUserIdAsReceiverUsername() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then — "주문자 = 수령자" 전제로 userId가 receiverUsername 자리에 그대로 들어가는지 검증
            verify(deliveryClient).createDelivery(argThat(request ->
                    request.receiverUsername().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("주문 생성 성공 시 배송 응답의 arrivalHubId가 hubId로 저장된다")
        void create_success_storesHubIdFromDeliveryResponse() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            verify(orderRepository).save(argThat(order -> HUB_ID.equals(order.getHubId())));
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelOrderTest {

        private Order pendingOrder;

        @BeforeEach
        void setUp() {
            pendingOrder = Order.createPending(
                    UUID.randomUUID(), REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID,
                    PRODUCT_ID, "마른오징어", 10, BigDecimal.valueOf(10000),
                    LocalDateTime.now().plusDays(3), "빨리요", HUB_ID
            );
        }

        @Test
        @DisplayName("취소 성공 - 마스터 관리자")
        void cancel_success_master() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(deliveryClient).cancelDelivery(any());
            given(inventoryClient.restoreStock(any(), any()))
                    .willReturn(new InventoryRestoreResponseDto(PRODUCT_ID, 100));

            // when
            orderService.cancelOrder(pendingOrder.getId(), null, "MASTER", null, "admin");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(deliveryClient).cancelDelivery(any());
            verify(inventoryClient).restoreStock(eq(PRODUCT_ID), any());
        }

        @Test
        @DisplayName("취소 성공 - 담당 허브 관리자")
        void cancel_success_hubManager_ownHub() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(deliveryClient).cancelDelivery(any());
            given(inventoryClient.restoreStock(any(), any()))
                    .willReturn(new InventoryRestoreResponseDto(PRODUCT_ID, 100));

            // when
            orderService.cancelOrder(pendingOrder.getId(), null, "HUB_MANAGER", HUB_ID, "hub01");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("취소 실패 - 담당 허브가 아닌 허브 관리자")
        void cancel_fail_hubManager_otherHub() {
            // given
            UUID otherHubId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), null, "HUB_MANAGER", otherHubId, "hub02")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            verifyNoInteractions(deliveryClient, inventoryClient);
        }

        @Test
        @DisplayName("취소 실패 - 허브 관리자가 hubId 없이 요청")
        void cancel_fail_hubManager_noHubId() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), null, "HUB_MANAGER", null, "hub01")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("취소 실패 - PENDING 상태가 아닌 주문")
        void cancel_fail_notPending() {
            // given
            Order deliveredOrder = Order.createPending(
                    UUID.randomUUID(), REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID,
                    PRODUCT_ID, "마른오징어", 10, BigDecimal.valueOf(10000),
                    LocalDateTime.now().plusDays(3), null, HUB_ID
            );
            ReflectionTestUtils.setField(deliveredOrder, "status", OrderStatus.DELIVERED);

            given(orderRepository.findByIdAndDeletedAtIsNull(deliveredOrder.getId()))
                    .willReturn(Optional.of(deliveredOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(deliveredOrder.getId(), null, "MASTER", null, "admin")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
            verifyNoInteractions(deliveryClient, inventoryClient);
        }

        @Test
        @DisplayName("취소 실패 - 존재하지 않는 주문")
        void cancel_fail_orderNotFound() {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(nonExistentId, null, "MASTER", null, "admin")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("취소 실패 - 배송 취소 실패 시 재고 복원이 호출되지 않는다")
        void cancel_fail_deliveryCancelFailed_thenNoRestoreStock() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION))
                    .given(deliveryClient).cancelDelivery(any());

            // when & then
            assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), null, "MASTER", null, "admin")
            );

            verifyNoInteractions(inventoryClient);
        }
    }
}