package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.order.domain.entity.DeliveryStatusMapper;
import com.bonae.logistics.order.domain.entity.Order;
import com.bonae.logistics.order.domain.entity.OrderStatus;
import com.bonae.logistics.order.domain.repository.OrderRepository;
import com.bonae.logistics.order.infrastructure.client.dto.response.DeliveryCreateResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryUpdateResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.ProductInfoResponseDto;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.request.OrderSearchCondition;
import com.bonae.logistics.order.presentation.dto.request.OrderUpdateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import com.bonae.logistics.order.presentation.dto.response.OrderSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderExternalCallRetryHelper retryHelper;

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request, UUID requesterCompanyId, String userId) {

        UUID orderId = UUID.randomUUID();

        // 상품 정보 조회(스냅샷용)
        ProductInfoResponseDto productInfo = retryHelper.getProductInfoWithRetry(request.productId());

        if (productInfo.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 재고 차감 실행
        InventoryUpdateResponseDto inventoryResult =
                retryHelper.deductStockWithRetry(orderId, request.productId(), request.quantity());

        String requestNote = buildRequestNote(request.dueDate(), request.remarks());
        DeliveryCreateResponseDto deliveryResult;
        try {
            deliveryResult = retryHelper.createDeliveryWithRetry(orderId, userId, request, productInfo, requestNote);
        } catch (BusinessException e) {
            log.error("배송 생성 실패, 보상 트랜잭션(재고 복원) 시작: orderId={}", orderId, e);

            InventoryUpdateResponseDto restoreResult =
                    retryHelper.restoreStockWithRetry(orderId, request.productId(), request.quantity());
            log.info("재고 복원 완료: orderId={}, inventoryId={}, afterQuantity={}",
                    orderId, restoreResult.inventoryId(), restoreResult.afterQuantity());

            throw e;
        }

        // 주문 저장
        Order order = Order.createPending(
                orderId,
                requesterCompanyId,
                request.receiverCompanyId(),
                request.productId(),
                productInfo.name(),
                request.quantity(),
                productInfo.price(),
                request.dueDate(),
                request.remarks(),
                deliveryResult.arrivalHubId()
        );
        orderRepository.save(order);

        return OrderResponseDto.from(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String userId, String userRole, UUID hubId) {
        // 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOwnership(order, userId, userRole, hubId);
        // 상태 검증
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "PENDING 상태의 주문만 취소할 수 있습니다.");
        }

        retryHelper.cancelDeliveryWithRetry(orderId);

        try {
            InventoryUpdateResponseDto restoreResult =
                    retryHelper.restoreStockWithRetry(orderId, order.getProductId(), order.getQuantity());
            log.info("주문 취소로 인한 재고 복원 완료: orderId={}, afterQuantity={}", orderId, restoreResult.afterQuantity());
            order.cancel(userId);
        } catch (BusinessException e) {
            log.error("재고 복원 최종 실패, 재고 정합성 이슈 상태이므로 주문은 취소 처리함: orderId={}", orderId, e);
            order.markCancelledWithInventoryIssue(userId);
        }
    }

    @Transactional
    public OrderResponseDto updateOrder(UUID orderId, String userId, OrderUpdateRequestDto request, String userRole, UUID hubId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOwnership(order, userId, userRole, hubId);

        order.update(request.dueDate(), request.remarks());

        String requestNote = buildRequestNote(order.getDueDate(), order.getRemarks());
        retryHelper.notifyDeliveryUpdateWithRetry(orderId, requestNote);

        return OrderResponseDto.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderSummaryResponseDto> getOrders(
            OrderSearchCondition condition, PageRequestDto pageRequestDto, String userRole, String userId, UUID userHubId) {
        UUID scopeHubId = null;
        String scopeUserId = null;

        if ("HUB_MANAGER".equals(userRole)) {
            scopeHubId = userHubId;
        } else if (!"MASTER".equals(userRole)) {
            scopeUserId = userId;
        }

        OrderStatus status = condition.status() != null ? OrderStatus.valueOf(condition.status()) : null;

        Page<Order> orders = orderRepository.search(status, scopeHubId, scopeUserId, pageRequestDto.toPageable());
        return PageResponseDto.from(orders, OrderSummaryResponseDto::from);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(UUID orderId, String userId, String userRole, UUID userHubId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateOwnership(order, userId, userRole, userHubId);

        return OrderResponseDto.from(order);
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, String deliveryStatus) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus mappedStatus = DeliveryStatusMapper.toOrderStatus(deliveryStatus);
        order.updateStatus(mappedStatus);
    }

    private String buildRequestNote(LocalDateTime dueDate, String remarks) {
        String dueDateText = dueDate.format(DateTimeFormatter.ofPattern("M월 d일 H시까지"));
        return remarks == null || remarks.isBlank()
                ? dueDateText + " 배송 부탁드립니다."
                : dueDateText + " " + remarks;
    }

    private void validateOwnership(Order order, String userId, String userRole, UUID hubId) {
        // MASTER는 전체 조회
        if ("MASTER".equals(userRole)) {
            return;
        }
        // 허브 담당자는 본인 담당 허브의 주문만 조회
        if ("HUB_MANAGER".equals(userRole)) {
            if (hubId == null || !hubId.equals(order.getHubId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        // 업체, 배송 담당자는 본인이 만든 주문만 조회
        if (!order.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}