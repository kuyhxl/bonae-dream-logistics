package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.order.domain.entity.Order;
import com.bonae.logistics.order.domain.entity.OrderStatus;
import com.bonae.logistics.order.domain.repository.OrderRepository;
import com.bonae.logistics.order.infrastructure.client.DeliveryClient;
import com.bonae.logistics.order.infrastructure.client.InventoryClient;
import com.bonae.logistics.order.infrastructure.client.ProductClient;
import com.bonae.logistics.order.infrastructure.client.SlackClient;
import com.bonae.logistics.order.infrastructure.client.dto.response.*;
import com.bonae.logistics.order.infrastructure.config.AlertProperties;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.request.OrderSearchCondition;
import com.bonae.logistics.order.presentation.dto.request.OrderUpdateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import com.bonae.logistics.order.presentation.dto.response.OrderSummaryResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
    @Mock private OrderExternalCallRetryHelper retryHelper;

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
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderResponseDto response = orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("PENDING");

            verify(retryHelper).getProductInfoWithRetry(PRODUCT_ID);
            verify(retryHelper).deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt());
            verify(retryHelper).createDeliveryWithRetry(any(), any(), any(), any(), any());
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("주문 생성 실패 - 삭제된 상품일 경우")
        void create_fail_productDeleted() {
            // given
            ProductInfoResponseDto deletedProduct = new ProductInfoResponseDto(
                    PRODUCT_ID, "삭제된상품", SUPPLIER_COMPANY_ID, BigDecimal.TEN, true
            );
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(deletedProduct);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
            verify(retryHelper, never()).deductStockWithRetry(any(), any(), anyInt());
            verify(retryHelper, never()).createDeliveryWithRetry(any(), any(), any(), any(), any());
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 재고 부족일 경우")
        void create_fail_stockShortage() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE));

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STOCK_SHORTAGE);
            verify(retryHelper, never()).createDeliveryWithRetry(any(), any(), any(), any(), any());
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 배송 생성 실패 시 재고 복원(보상 트랜잭션) 수행")
        void create_fail_deliveryFailed_thenRestoreStock() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
                    .willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
            given(retryHelper.restoreStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 90, 10, 100, "RESTORE", null));

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
            verify(retryHelper, times(1)).restoreStockWithRetry(any(), eq(PRODUCT_ID), anyInt());
            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("주문 생성 실패 - 배송 생성 및 재고 복원이 모두 실패하면 원래 에러가 전파된다")
        void create_fail_deliveryAndRestoreBothFailed() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
                    .willThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE));
            given(retryHelper.restoreStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
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
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
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
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), eq(productInfoResponseDto), any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then — createDeliveryWithRetry에 넘어간 productInfo의 companyId가 맞는지 검증
            verify(retryHelper).createDeliveryWithRetry(any(), any(), any(),
                    argThat(info -> info.companyId().equals(SUPPLIER_COMPANY_ID)), any());
        }

        @Test
        @DisplayName("배송 생성 요청 시 receiverUsername(userId)이 그대로 전달된다")
        void create_deliveryRequest_usesUserIdAsReceiverUsername() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), eq(USER_ID), any(), any(), any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            verify(retryHelper).createDeliveryWithRetry(any(), eq(USER_ID), any(), any(), any());
        }

        @Test
        @DisplayName("주문 생성 성공 시 배송 응답의 arrivalHubId가 hubId로 저장된다")
        void create_success_storesHubIdFromDeliveryResponse() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            verify(orderRepository).save(argThat(order -> HUB_ID.equals(order.getHubId())));
        }

        @Test
        @DisplayName("배송 생성 요청 시 productName, quantity, dueDate가 개별 필드로 전달된다")
        void create_deliveryRequest_sendsIndividualFields() {
            // given
            given(retryHelper.getProductInfoWithRetry(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(retryHelper.deductStockWithRetry(any(), eq(PRODUCT_ID), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 100, 10, 90, "DECREASE", null));
            given(retryHelper.createDeliveryWithRetry(any(), any(), any(), any(), any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, HUB_ID, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID, USER_ID);

            // then
            verify(retryHelper).createDeliveryWithRetry(
                    any(),
                    eq(USER_ID),
                    eq(requestDto),
                    eq(productInfoResponseDto),
                    any()
            );
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
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(retryHelper).cancelDeliveryWithRetry(any());
            given(retryHelper.restoreStockWithRetry(any(), any(), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 90, 10, 100, "RESTORE", null));

            // when
            orderService.cancelOrder(pendingOrder.getId(), "admin", "MASTER", null);

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(retryHelper).cancelDeliveryWithRetry(any());
            verify(retryHelper).restoreStockWithRetry(any(), eq(PRODUCT_ID), anyInt());
        }

        @Test
        @DisplayName("취소 성공 - 담당 허브 관리자")
        void cancel_success_hubManager_ownHub() {
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(retryHelper).cancelDeliveryWithRetry(any());
            given(retryHelper.restoreStockWithRetry(any(), any(), anyInt()))
                    .willReturn(new InventoryUpdateResponseDto(null, PRODUCT_ID, 90, 10, 100, "RESTORE", null));

            // when
            orderService.cancelOrder(pendingOrder.getId(), "hub01", "HUB_MANAGER", HUB_ID);

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("취소 실패 - 담당 허브가 아닌 허브 관리자")
        void cancel_fail_hubManager_otherHub() {
            UUID otherHubId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), "hub02", "HUB_MANAGER", otherHubId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            verifyNoInteractions(retryHelper);
        }

        @Test
        @DisplayName("취소 실패 - 허브 관리자가 hubId 없이 요청")
        void cancel_fail_hubManager_noHubId() {
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), "hub01", "HUB_MANAGER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("취소 실패 - PENDING 상태가 아닌 주문")
        void cancel_fail_notPending() {
            Order deliveredOrder = Order.createPending(
                    UUID.randomUUID(), REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID,
                    PRODUCT_ID, "마른오징어", 10, BigDecimal.valueOf(10000),
                    LocalDateTime.now().plusDays(3), null, HUB_ID
            );
            ReflectionTestUtils.setField(deliveredOrder, "status", OrderStatus.DELIVERED);

            given(orderRepository.findByIdAndDeletedAtIsNull(deliveredOrder.getId()))
                    .willReturn(Optional.of(deliveredOrder));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(deliveredOrder.getId(), "admin", "MASTER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
            verifyNoInteractions(retryHelper);
        }

        @Test
        @DisplayName("취소 실패 - 존재하지 않는 주문")
        void cancel_fail_orderNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                    .willReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(nonExistentId, "admin", "MASTER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("취소 실패 - 배송 취소 실패 시 재고 복원이 호출되지 않는다")
        void cancel_fail_deliveryCancelFailed_thenNoRestoreStock() {
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION))
                    .given(retryHelper).cancelDeliveryWithRetry(any());

            assertThrows(
                    BusinessException.class,
                    () -> orderService.cancelOrder(pendingOrder.getId(), "admin", "MASTER", null)
            );

            verify(retryHelper, never()).restoreStockWithRetry(any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("주문 수정 테스트")
    class UpdateOrderTest {

        private Order pendingOrder;

        @BeforeEach
        void setUp() {
            pendingOrder = Order.createPending(
                    UUID.randomUUID(), REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID,
                    PRODUCT_ID, "마른오징어", 10, BigDecimal.valueOf(10000),
                    LocalDateTime.now().plusDays(3), "기존 요청사항", HUB_ID
            );
        }

        @Test
        @DisplayName("수정 성공 - dueDate, remarks 둘 다 변경")
        void update_success_both() {
            LocalDateTime newDueDate = LocalDateTime.now().plusDays(7);
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(newDueDate, "변경된 요청사항");

            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(retryHelper).notifyDeliveryUpdateWithRetry(any(), any());

            // when
            OrderResponseDto response = orderService.updateOrder(pendingOrder.getId(), "admin", request, "MASTER", null);

            // then
            assertThat(response.dueDate()).isEqualTo(newDueDate);
            assertThat(response.remarks()).isEqualTo("변경된 요청사항");
            verify(retryHelper).notifyDeliveryUpdateWithRetry(any(), any());
        }

        @Test
        @DisplayName("수정 성공 - dueDate만 변경 (remarks는 null)")
        void update_success_dueDateOnly() {
            LocalDateTime newDueDate = LocalDateTime.now().plusDays(7);
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(newDueDate, null);

            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(retryHelper).notifyDeliveryUpdateWithRetry(any(), any());

            // when
            OrderResponseDto response = orderService.updateOrder(pendingOrder.getId(), "admin", request, "MASTER", null);

            // then
            assertThat(response.dueDate()).isEqualTo(newDueDate);
            assertThat(response.remarks()).isEqualTo("기존 요청사항");
        }

        @Test
        @DisplayName("수정 성공 - 담당 허브 관리자")
        void update_success_hubManager_ownHub() {
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(LocalDateTime.now().plusDays(5), "수정됨");

            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));
            willDoNothing().given(retryHelper).notifyDeliveryUpdateWithRetry(any(), any());

            // when
            OrderResponseDto response = orderService.updateOrder(pendingOrder.getId(), "hub01", request, "HUB_MANAGER", HUB_ID);

            // then
            assertThat(response.remarks()).isEqualTo("수정됨");
        }

        @Test
        @DisplayName("수정 실패 - 담당 허브가 아닌 허브 관리자")
        void update_fail_hubManager_otherHub() {
            UUID otherHubId = UUID.randomUUID();
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(LocalDateTime.now().plusDays(5), "수정됨");

            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrder(pendingOrder.getId(), "hub02", request, "HUB_MANAGER", otherHubId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            verifyNoInteractions(retryHelper);
        }

        @Test
        @DisplayName("수정 실패 - PENDING 상태가 아닌 주문")
        void update_fail_notPending() {
            ReflectionTestUtils.setField(pendingOrder, "status", OrderStatus.DELIVERED);
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(LocalDateTime.now().plusDays(5), "수정됨");

            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrder(pendingOrder.getId(), "admin", request, "MASTER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
            verifyNoInteractions(retryHelper);
        }

        @Test
        @DisplayName("수정 실패 - 존재하지 않는 주문")
        void update_fail_orderNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            OrderUpdateRequestDto request = new OrderUpdateRequestDto(LocalDateTime.now().plusDays(5), "수정됨");

            given(orderRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                    .willReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrder(nonExistentId, "admin", request, "MASTER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("주문 목록 조회 테스트")
    class GetOrdersTest {

        private PageRequestDto createPageRequest() {
            PageRequestDto dto = new PageRequestDto();
            dto.setPage(1);
            dto.setSize(10);
            dto.setSort("createdAt");
            dto.setDirection("desc");
            return dto;
        }

        @Test
        @DisplayName("목록 조회 성공 - 마스터 관리자는 전체 조회")
        void getOrders_success_master() {
            // given
            OrderSearchCondition condition = new OrderSearchCondition(null);
            PageRequestDto pageRequestDto = createPageRequest();
            Page<Order> emptyPage = Page.empty();

            given(orderRepository.search(isNull(), isNull(), isNull(), any()))
                    .willReturn(emptyPage);

            // when
            PageResponseDto<OrderSummaryResponseDto> response =
                    orderService.getOrders(condition, pageRequestDto, "MASTER", "admin", null);

            // then
            assertThat(response.getTotalElements()).isEqualTo(0);
            verify(orderRepository).search(isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("목록 조회 성공 - 허브 관리자는 담당 허브로 스코프 필터링")
        void getOrders_success_hubManager_scopedByHub() {
            // given
            OrderSearchCondition condition = new OrderSearchCondition(null);
            PageRequestDto pageRequestDto = createPageRequest();
            Page<Order> emptyPage = Page.empty();

            given(orderRepository.search(isNull(), eq(HUB_ID), isNull(), any()))
                    .willReturn(emptyPage);

            // when
            orderService.getOrders(condition, pageRequestDto, "HUB_MANAGER", "hub01", HUB_ID);

            // then
            verify(orderRepository).search(isNull(), eq(HUB_ID), isNull(), any());
        }

        @Test
        @DisplayName("목록 조회 성공 - 업체 담당자는 본인 username으로 스코프 필터링")
        void getOrders_success_companyManager_scopedByUserId() {
            // given
            OrderSearchCondition condition = new OrderSearchCondition(null);
            PageRequestDto pageRequestDto = createPageRequest();
            Page<Order> emptyPage = Page.empty();

            given(orderRepository.search(isNull(), isNull(), eq("company01"), any()))
                    .willReturn(emptyPage);

            // when
            orderService.getOrders(condition, pageRequestDto, "COMPANY_MANAGER", "company01", null);

            // then
            verify(orderRepository).search(isNull(), isNull(), eq("company01"), any());
        }

        @Test
        @DisplayName("목록 조회 성공 - status 검색 조건이 적용된다")
        void getOrders_success_withStatusCondition() {
            // given
            OrderSearchCondition condition = new OrderSearchCondition("PENDING");
            PageRequestDto pageRequestDto = createPageRequest();
            Page<Order> emptyPage = Page.empty();

            given(orderRepository.search(eq(OrderStatus.PENDING), any(), any(), any()))
                    .willReturn(emptyPage);

            // when
            orderService.getOrders(condition, pageRequestDto, "MASTER", "admin", null);

            // then
            verify(orderRepository).search(eq(OrderStatus.PENDING), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("주문 단건 조회 테스트")
    class GetOrderTest {

        private Order pendingOrder;

        @BeforeEach
        void setUp() {
            pendingOrder = Order.createPending(
                    UUID.randomUUID(), REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID,
                    PRODUCT_ID, "마른오징어", 10, BigDecimal.valueOf(10000),
                    LocalDateTime.now().plusDays(3), "빨리요", HUB_ID
            );
            ReflectionTestUtils.setField(pendingOrder, "createdBy", "company01");
        }

        @Test
        @DisplayName("단건 조회 성공 - 마스터 관리자")
        void getOrder_success_master() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            OrderResponseDto response = orderService.getOrder(pendingOrder.getId(), "admin", "MASTER", null);

            // then
            assertThat(response.id()).isEqualTo(pendingOrder.getId());
        }

        @Test
        @DisplayName("단건 조회 성공 - 담당 허브 관리자")
        void getOrder_success_hubManager_ownHub() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            OrderResponseDto response = orderService.getOrder(pendingOrder.getId(), "hub01", "HUB_MANAGER", HUB_ID);

            // then
            assertThat(response.id()).isEqualTo(pendingOrder.getId());
        }

        @Test
        @DisplayName("단건 조회 실패 - 담당 허브가 아닌 허브 관리자")
        void getOrder_fail_hubManager_otherHub() {
            // given
            UUID otherHubId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.getOrder(pendingOrder.getId(), "hub02", "HUB_MANAGER", otherHubId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("단건 조회 성공 - 본인이 만든 주문(업체 담당자)")
        void getOrder_success_companyManager_ownOrder() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            OrderResponseDto response = orderService.getOrder(pendingOrder.getId(), "company01", "COMPANY_MANAGER", null);

            // then
            assertThat(response.id()).isEqualTo(pendingOrder.getId());
        }

        @Test
        @DisplayName("단건 조회 실패 - 본인이 만들지 않은 주문(업체 담당자)")
        void getOrder_fail_companyManager_notOwnOrder() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.getOrder(pendingOrder.getId(), "company02", "COMPANY_MANAGER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("단건 조회 실패 - 존재하지 않는 주문")
        void getOrder_fail_orderNotFound() {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.getOrder(nonExistentId, "admin", "MASTER", null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("주문 상태 변경(내부 콜백) 테스트")
    class UpdateOrderStatusTest {

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
        @DisplayName("상태 변경 성공 - READY/HUB_WAITING → PENDING")
        void updateStatus_success_toPending() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            orderService.updateOrderStatus(pendingOrder.getId(), "HUB_WAITING");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("상태 변경 성공 - HUB_MOVING → IN_TRANSIT")
        void updateStatus_success_toInTransit() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            orderService.updateOrderStatus(pendingOrder.getId(), "HUB_MOVING");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        }

        @Test
        @DisplayName("상태 변경 성공 - OUT_FOR_DELIVERY → IN_TRANSIT")
        void updateStatus_success_outForDelivery_toInTransit() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            orderService.updateOrderStatus(pendingOrder.getId(), "OUT_FOR_DELIVERY");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        }

        @Test
        @DisplayName("상태 변경 성공 - DELIVERED → DELIVERED")
        void updateStatus_success_toDelivered() {
            // given
            ReflectionTestUtils.setField(pendingOrder, "status", OrderStatus.IN_TRANSIT);
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            orderService.updateOrderStatus(pendingOrder.getId(), "DELIVERED");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("상태 변경 성공 - CANCELLED → CANCELLED")
        void updateStatus_success_toCancelled() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when
            orderService.updateOrderStatus(pendingOrder.getId(), "CANCELLED");

            // then
            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("상태 변경 실패 - 이미 DELIVERED인 주문")
        void updateStatus_fail_alreadyDelivered() {
            // given
            ReflectionTestUtils.setField(pendingOrder, "status", OrderStatus.DELIVERED);
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrderStatus(pendingOrder.getId(), "HUB_MOVING")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("상태 변경 실패 - 이미 CANCELLED인 주문")
        void updateStatus_fail_alreadyCancelled() {
            // given
            ReflectionTestUtils.setField(pendingOrder, "status", OrderStatus.CANCELLED);
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrderStatus(pendingOrder.getId(), "HUB_WAITING")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("상태 변경 실패 - IN_TRANSIT에서 PENDING으로 역행")
        void updateStatus_fail_backwardTransition() {
            // given
            ReflectionTestUtils.setField(pendingOrder, "status", OrderStatus.IN_TRANSIT);
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrderStatus(pendingOrder.getId(), "HUB_WAITING")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("상태 변경 실패 - 매핑되지 않는 상태값")
        void updateStatus_fail_unmappedStatus() {
            // given
            given(orderRepository.findByIdAndDeletedAtIsNull(pendingOrder.getId()))
                    .willReturn(Optional.of(pendingOrder));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrderStatus(pendingOrder.getId(), "UNKNOWN_STATUS")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("상태 변경 실패 - 존재하지 않는 주문")
        void updateStatus_fail_orderNotFound() {
            // given
            UUID nonExistentId = UUID.randomUUID();
            given(orderRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> orderService.updateOrderStatus(nonExistentId, "HUB_MOVING")
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }
    }
}