package com.bonae.logistics.order.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.order.infrastructure.client.InventoryClient;
import com.bonae.logistics.order.infrastructure.client.dto.request.InventoryUpdateRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventorySearchResponseDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.InventoryUpdateResponseDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class OrderExternalCallRetryHelperIntegrationTest {

    @Autowired
    private OrderExternalCallRetryHelper retryHelper;

    @MockitoBean
    private InventoryClient inventoryClient;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID INVENTORY_ID = UUID.randomUUID();

    @Nested
    @DisplayName("재고 차감 재시도 통합 테스트")
    class DeductStockRetryTest {

        @Test
        @DisplayName("재고 차감 성공 시 1번만 호출된다")
        void deductStock_success_calledOnce() {
            // given
            given(inventoryClient.searchInventory(PRODUCT_ID))
                    .willReturn(new InventorySearchResponseDto(
                            List.of(new InventorySearchResponseDto.InventoryItem(INVENTORY_ID, PRODUCT_ID, null, 100))
                    ));
            given(inventoryClient.updateInventory(any(), any()))
                    .willReturn(new InventoryUpdateResponseDto(null, INVENTORY_ID, 100, 10, 90, "DECREASE", null));

            // when
            retryHelper.deductStockWithRetry(UUID.randomUUID(), PRODUCT_ID, 10);

            // then
            verify(inventoryClient, times(1)).updateInventory(any(), any());
        }

        @Test
        @DisplayName("재고 차감 3번 모두 실패 시 재시도 후 원래 예외가 전파된다")
        void deductStock_fail_retriesThreeTimesAndThrows() {
            // given
            given(inventoryClient.searchInventory(PRODUCT_ID))
                    .willReturn(new InventorySearchResponseDto(
                            List.of(new InventorySearchResponseDto.InventoryItem(INVENTORY_ID, PRODUCT_ID, null, 100))
                    ));
            given(inventoryClient.updateInventory(any(), any()))
                    .willThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE));

            // when & then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> retryHelper.deductStockWithRetry(UUID.randomUUID(), PRODUCT_ID, 10)
            );

            verify(inventoryClient, times(3)).updateInventory(any(), any());
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}