package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.domain.entity.Order;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

        @Test
        @DisplayName("주문 생성 성공")
        void create_success() {
            // given
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, null, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderResponseDto response = orderService.createOrder(requestDto, REQUESTER_COMPANY_ID);

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
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID)
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
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID)
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
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID)
            );

            // then — 배송 생성 실패 원인(COMPANY_NOT_FOUND)이 그대로 전파되어야 함
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            // 보상 트랜잭션(재고 복원)이 정확히 1번 호출됐는지 검증
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
                    () -> orderService.createOrder(requestDto, REQUESTER_COMPANY_ID)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("주문 생성 성공 - 요청업체(requesterCompanyId)가 없는 경우도 허용")
        void create_success_withoutRequesterCompanyId() {
            // given — 마스터/허브 관리자처럼 소속 업체가 없는 경우
            given(productClient.getProductInfo(PRODUCT_ID)).willReturn(productInfoResponseDto);
            given(inventoryClient.deductStock(eq(PRODUCT_ID), any()))
                    .willReturn(new InventoryDeductResponseDto(PRODUCT_ID, 90));
            given(deliveryClient.createDelivery(any()))
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, null, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderResponseDto response = orderService.createOrder(requestDto, null);

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
                    .willReturn(new DeliveryCreateResponseDto(DELIVERY_ID, "HUB_WAITING", null, null, 3));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(requestDto, REQUESTER_COMPANY_ID);

            // then — requesterCompanyId가 아니라 상품의 companyId(SUPPLIER_COMPANY_ID)가 쓰였는지 검증
            verify(deliveryClient).createDelivery(argThat(request ->
                    request.supplierCompanyId().equals(SUPPLIER_COMPANY_ID)
            ));
        }
    }
}