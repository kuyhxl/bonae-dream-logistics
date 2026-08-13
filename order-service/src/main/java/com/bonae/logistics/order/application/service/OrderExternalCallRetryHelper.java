package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.infrastructure.client.DeliveryClient;
import com.bonae.logistics.order.infrastructure.client.InventoryClient;
import com.bonae.logistics.order.infrastructure.client.ProductClient;
import com.bonae.logistics.order.infrastructure.client.SlackClient;
import com.bonae.logistics.order.infrastructure.client.dto.request.*;
import com.bonae.logistics.order.infrastructure.client.dto.response.*;
import com.bonae.logistics.order.infrastructure.config.AlertProperties;
import com.bonae.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExternalCallRetryHelper {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final DeliveryClient deliveryClient;
    private final SlackClient slackClient;
    private final AlertProperties alertProperties;

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
    public InventoryUpdateResponseDto deductStockWithRetry(UUID orderId, UUID productId, int quantity) {
        UUID inventoryId = findInventoryId(productId);
        return inventoryClient.updateInventory(inventoryId, new InventoryUpdateRequestDto(orderId, quantity, "DECREASE"));
    }

    @Recover
    public InventoryUpdateResponseDto recoverDeduct(BusinessException e, UUID orderId, UUID productId, int quantity) {
        log.error("재고 차감 재시도 모두 실패: orderId={}", orderId);
        throw e;
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public InventoryUpdateResponseDto restoreStockWithRetry(UUID orderId, UUID productId, int quantity) {
        UUID inventoryId = findInventoryId(productId);
        return inventoryClient.updateInventory(inventoryId, new InventoryUpdateRequestDto(orderId, quantity, "RESTORE"));
    }

    @Recover
    public InventoryUpdateResponseDto recoverRestore(BusinessException e, UUID orderId, UUID productId, int quantity) {
        log.error("보상 트랜잭션(재고 복원) 최종 실패. 수동 개입 필요: orderId={}, productId={}", orderId, productId);
        notifySlackForManualIntervention(orderId, productId, quantity);
        throw new BusinessException(ErrorCode.ORDER_CREATION_FAILED);
    }

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public DeliveryCreateResponseDto createDeliveryWithRetry(
            UUID orderId, String userId, OrderCreateRequestDto request, ProductInfoResponseDto productInfo, String requestNote
    ) {
        String productInfoText = productInfo.name() + " " + request.quantity() + "개";

        return deliveryClient.createDelivery(new DeliveryCreateRequestDto(
                orderId,
                productInfo.companyId(),
                request.receiverCompanyId(),
                userId,
                productInfoText,
                requestNote
        ));
    }

    @Recover
    public DeliveryCreateResponseDto recoverDelivery(
            BusinessException e, UUID orderId, String userId, OrderCreateRequestDto request, ProductInfoResponseDto productInfo, String requestNote
    ) {
        log.error("배송 생성 재시도 모두 실패: orderId={}", orderId);
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

    @Retryable(retryFor = {BusinessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void notifyDeliveryUpdateWithRetry(UUID orderId, String requestNote) {
        deliveryClient.updateDelivery(new DeliveryUpdateRequestDto(orderId, requestNote));
    }

    @Recover
    public void recoverNotifyDeliveryUpdate(BusinessException e, UUID orderId, String requestNote) {
        log.error("배송 정보 갱신 알림 실패: orderId={}", orderId);
        throw e;
    }

    private void notifySlackForManualIntervention(UUID orderId, UUID productId, int quantity) {
        String message = String.format(
                "[긴급] 재고 복원 실패\n주문 ID: %s\n상품 ID: %s\n수량: %d\n수동 확인이 필요합니다.",
                orderId, productId, quantity);
        try {
            slackClient.sendMessage(new SlackMessageRequestDto(alertProperties.getAdminSlackId(), message));
        } catch (Exception slackError) {
            log.error("슬랙 알림 발송 실패: orderId={}", orderId, slackError);
        }
    }

    private UUID findInventoryId(UUID productId) {
        InventorySearchResponseDto response = inventoryClient.searchInventory(productId);
        if (response.content().isEmpty()) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND);
        }
        return response.content().get(0).inventoryId();
    }
}