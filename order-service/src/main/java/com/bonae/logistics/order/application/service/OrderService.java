package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.domain.entity.Order;
import com.bonae.logistics.order.domain.entity.OrderStatus;
import com.bonae.logistics.order.domain.repository.OrderRepository;
import com.bonae.logistics.order.infrastructure.client.*;
import com.bonae.logistics.order.infrastructure.client.dto.request.*;
import com.bonae.logistics.order.infrastructure.client.dto.response.DeliveryCreateResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryDeductResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryRestoreResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.ProductInfoResponseDto;
import com.bonae.logistics.order.infrastructure.config.AlertProperties;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.bonae.logistics.order.presentation.dto.response.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final DeliveryClient deliveryClient;
    private final SlackClient slackClient;
    private final AlertProperties alertProperties;

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request, UUID requesterCompanyId, String userId) {

        UUID orderId = UUID.randomUUID();

        // 상품 정보 조회(스냅샷용)
        ProductInfoResponseDto productInfo = getProductInfoWithRetry(request.productId());

        if(productInfo.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 재고 차감 실행
        InventoryDeductResponseDto inventoryResult = deductStockWitRetry(orderId, request.productId(), request.quantity());

        DeliveryCreateResponseDto deliveryResult;
        try {
            deliveryResult = createDeliveryWithRetry(orderId, requesterCompanyId, userId, request, productInfo);
        } catch (BusinessException e) {
            log.error("배송 생성 실패, 보상 트랜잭션(재고 복원) 시작: orderId={}", orderId, e);

            InventoryRestoreResponseDto restoreResult = restoreStockWithRetry(orderId, request.productId(), request.quantity());
            log.info("재고 복원 완료: orderId={}, productId={}, remainingStock={}",
                    orderId, restoreResult.productId(), restoreResult.remainingStock());

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
    public void cancelOrder(UUID orderId, UUID requesterCompanyId,String userRole,UUID hubId, String userId) {
        // 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validateHubScopeIfNeeded(order, userRole, hubId);
        // 상태 검증
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "PENDING 상태의 주문만 취소할 수 있습니다.");
        }

        cancelDeliveryWithRetry(orderId);

        InventoryRestoreResponseDto restoreResult = restoreStockWithRetry(orderId, order.getProductId(), order.getQuantity());
        log.info("주문 취소로 인한 재고 복원 완료: orderId={}, remainingStock={}", orderId, restoreResult.remainingStock());

        order.cancel(userId);
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public InventoryDeductResponseDto deductStockWitRetry(UUID orderId, UUID productId, int quantity) {
        return inventoryClient.deductStock(productId, new InventoryDeductRequestDto(orderId, quantity));
    }

    @Recover
    public InventoryDeductResponseDto recover(BusinessException e, UUID orderId, UUID productId, int quantity) {
        log.error("재고 차감 재시도 모두 실패: orderId={}", orderId);
        throw e;
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public DeliveryCreateResponseDto createDeliveryWithRetry(
            UUID orderId, UUID requesterCompanyId, String userId, OrderCreateRequestDto request, ProductInfoResponseDto productInfo
    ) {
        String productInfoText = productInfo.name() + " " + request.quantity() + "개";

        return deliveryClient.createDelivery(new DeliveryCreateRequestDto(
                orderId,
                productInfo.companyId(),
                request.receiverCompanyId(),
                userId,
                productInfoText,
                request.remarks()
        ));
    }

    @Recover
    public DeliveryCreateResponseDto recoverDelivery(BusinessException e, UUID orderId, UUID requesterCompanyId, OrderCreateRequestDto request) {
        log.error("배송 생성 재시도 모두 실패: orderId={}", orderId);
        throw e;
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public InventoryRestoreResponseDto restoreStockWithRetry(UUID orderId, UUID productId, int quantity) {
        return inventoryClient.restoreStock(productId, new InventoryRestoreRequestDto(orderId, quantity));
    }

    @Recover
    public void recoverRestore(BusinessException e, UUID orderId, UUID productId, int quantity) {
        log.error("보상 트랜잭션(재고 복원) 최종 실패. 수동 개입 필요: orderId={}, productId={}", orderId, productId);
        notifySlackForManualIntervention(orderId, productId, quantity);
        throw new BusinessException(ErrorCode.ORDER_CREATION_FAILED);
    }

    private void notifySlackForManualIntervention(UUID orderId, UUID productId, int quantity) {
        String message = String.format(
                "[긴급] 재고 복원 실패\n주문 ID: %s\n상품 ID: %s\n수량: %d\n수동 확인이 필요합니다.",
                orderId, productId, quantity);

        try {
            slackClient.sendMessage(new SlackMessageRequestDto(
                    alertProperties.getAdminSlackId(),
                    message
            ));
        } catch (Exception slackError) {
            log.error("슬랙 알림 발송 실패: orderId={}", orderId, slackError);
        }
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ProductInfoResponseDto getProductInfoWithRetry(UUID productId) {
        return productClient.getProductInfo(productId);
    }

    @Recover
    public ProductInfoResponseDto recoverProductInfo(BusinessException e, UUID productId) {
        log.error("상품 조회 재시도 모두 실패: productId={}", productId);
        throw e;
    }


    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void cancelDeliveryWithRetry(UUID orderId) {
        deliveryClient.cancelDelivery(new DeliveryCancelRequestDto(orderId));
    }

    @Recover
    public void recoverCancelDelivery(BusinessException e, UUID orderId) {
        log.error("배송 취소 재시도 모두 실패: orderId={}", orderId);
        throw e;
    }

    //허브 관리자의 경우 본인 담당의 허브인지 검증
    private void validateHubScopeIfNeeded(Order order, String UserRole, UUID hubId) {
        if ("HUB_MANAGER".equals(UserRole)) {
            if (hubId == null || !hubId.equals(order.getHubId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
    }
}

