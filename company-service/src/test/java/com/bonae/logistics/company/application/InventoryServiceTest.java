package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.InventoryChangeType;
import com.bonae.logistics.company.domain.entity.InventoryIdempotencyKey;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.InventoryIdempotencyKeyRepository;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateInventoryDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateInventoryDto;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryIdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("createInventory_동일상품허브조합이없을때_재고생성성공")
    void createInventory_동일상품허브조합이없을때_재고생성성공() {
        Product product = productWithId();
        UUID hubId = UUID.randomUUID();

        when(inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId))
                .thenReturn(false);
        when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory inventory = inventoryService.createInventory(product, hubId, 100);

        assertThat(inventory.getProduct()).isEqualTo(product);
        assertThat(inventory.getHubId()).isEqualTo(hubId);
        assertThat(inventory.getQuantity()).isEqualTo(100);
        verify(inventoryRepository).saveAndFlush(any(Inventory.class));
    }

    @Test
    @DisplayName("createInventory_동일상품과허브조합이이미존재할때_예외발생")
    void createInventory_동일상품과허브조합이이미존재할때_예외발생() {
        Product product = productWithId();
        UUID hubId = UUID.randomUUID();

        when(inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId))
                .thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(product, hubId, 100))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_DUPLICATED);

        verify(inventoryRepository, never()).saveAndFlush(any(Inventory.class));
    }

    @Test
    @DisplayName("createInventory_사전검증통과후저장시점에상품허브유니크제약조건위반이발생할때_예외발생")
    void createInventory_사전검증통과후저장시점에유니크제약조건위반이발생할때_예외발생() {
        Product product = productWithId();
        UUID hubId = UUID.randomUUID();

        when(inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId))
                .thenReturn(false);
        when(inventoryRepository.saveAndFlush(any(Inventory.class)))
                .thenThrow(duplicateKeyException("ux_p_inventories_product_hub_active"));

        assertThatThrownBy(() -> inventoryService.createInventory(product, hubId, 100))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_DUPLICATED);
    }

    @Test
    @DisplayName("createInventory_저장시점에상품허브제약조건이아닌다른제약조건위반이발생할때_원본예외그대로전파")
    void createInventory_다른제약조건위반이발생할때_원본예외그대로전파() {
        Product product = productWithId();
        UUID hubId = UUID.randomUUID();

        when(inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId))
                .thenReturn(false);
        when(inventoryRepository.saveAndFlush(any(Inventory.class)))
                .thenThrow(duplicateKeyException("pk_p_inventories"));

        assertThatThrownBy(() -> inventoryService.createInventory(product, hubId, 100))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updateInventory_type이DECREASE나RESTORE가아닐때_예외발생")
    void updateInventory_유효하지않은type_예외발생() {
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 10, "INVALID");

        assertThatThrownBy(() -> inventoryService.updateInventory(UUID.randomUUID(), reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INVENTORY_TYPE);

        verifyNoInteractions(inventoryRepository, idempotencyKeyRepository);
    }

    @Test
    @DisplayName("updateInventory_quantity가0이하일때_예외발생")
    void updateInventory_quantity가0이하_예외발생() {
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 0, "DECREASE");

        assertThatThrownBy(() -> inventoryService.updateInventory(UUID.randomUUID(), reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        verifyNoInteractions(inventoryRepository, idempotencyKeyRepository);
    }

    @Test
    @DisplayName("updateInventory_quantity가null일때_예외발생")
    void updateInventory_quantity가null_예외발생() {
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), null, "DECREASE");

        assertThatThrownBy(() -> inventoryService.updateInventory(UUID.randomUUID(), reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("updateInventory_재고가없거나삭제됐을때_예외발생")
    void updateInventory_재고없음_예외발생() {
        UUID inventoryId = UUID.randomUUID();
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 10, "DECREASE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateInventory(inventoryId, reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);

        verifyNoInteractions(idempotencyKeyRepository);
    }

    @Test
    @DisplayName("updateInventory_최초요청DECREASE_정상적으로차감하고스냅샷을저장한다")
    void updateInventory_최초요청DECREASE_정상처리() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory before = inventoryWithId(inventoryId, product, 100);
        Inventory after = inventoryWithId(inventoryId, product, 50);
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 50, "DECREASE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("DECREASE")))
                .thenReturn(1);
        when(inventoryRepository.decreaseQuantity(inventoryId, 50)).thenReturn(1);

        ResUpdateInventoryDto result = inventoryService.updateInventory(inventoryId, reqDto);

        assertThat(result.getOrderId()).isEqualTo(reqDto.getOrderId());
        assertThat(result.getInventoryId()).isEqualTo(inventoryId);
        assertThat(result.getBeforeQuantity()).isEqualTo(100);
        assertThat(result.getChangedQuantity()).isEqualTo(50);
        assertThat(result.getAfterQuantity()).isEqualTo(50);
        assertThat(result.getType()).isEqualTo("DECREASE");

        verify(inventoryRepository).decreaseQuantity(inventoryId, 50);
        verify(inventoryRepository, never()).increaseQuantity(any(), anyInt());
        verify(idempotencyKeyRepository).fillSnapshot(any(UUID.class), eq(100), eq(50));
    }

    @Test
    @DisplayName("updateInventory_최초요청RESTORE_정상적으로복구하고스냅샷을저장한다")
    void updateInventory_최초요청RESTORE_정상처리() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory before = inventoryWithId(inventoryId, product, 50);
        Inventory after = inventoryWithId(inventoryId, product, 80);
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 30, "RESTORE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("RESTORE")))
                .thenReturn(1);
        when(inventoryRepository.increaseQuantity(inventoryId, 30)).thenReturn(1);

        ResUpdateInventoryDto result = inventoryService.updateInventory(inventoryId, reqDto);

        assertThat(result.getBeforeQuantity()).isEqualTo(50);
        assertThat(result.getChangedQuantity()).isEqualTo(30);
        assertThat(result.getAfterQuantity()).isEqualTo(80);
        assertThat(result.getType()).isEqualTo("RESTORE");

        verify(inventoryRepository).increaseQuantity(inventoryId, 30);
        verify(inventoryRepository, never()).decreaseQuantity(any(), anyInt());
        verify(idempotencyKeyRepository).fillSnapshot(any(UUID.class), eq(50), eq(80));
    }

    @Test
    @DisplayName("updateInventory_DECREASE인데재고가부족할때_예외발생하고스냅샷을채우지않는다")
    void updateInventory_DECREASE재고부족_예외발생() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory inventory = inventoryWithId(inventoryId, product, 10);
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 50, "DECREASE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)).thenReturn(Optional.of(inventory));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("DECREASE")))
                .thenReturn(1);
        when(inventoryRepository.decreaseQuantity(inventoryId, 50)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.updateInventory(inventoryId, reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_SHORTAGE);

        verify(idempotencyKeyRepository, never()).fillSnapshot(any(), any(), any());
    }

    @Test
    @DisplayName("updateInventory_RESTORE인데원자적UPDATE가0행일때_예외발생")
    void updateInventory_RESTORE_UPDATE0행_예외발생() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory inventory = inventoryWithId(inventoryId, product, 10);
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 5, "RESTORE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)).thenReturn(Optional.of(inventory));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("RESTORE")))
                .thenReturn(1);
        when(inventoryRepository.increaseQuantity(inventoryId, 5)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.updateInventory(inventoryId, reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);

        verify(idempotencyKeyRepository, never()).fillSnapshot(any(), any(), any());
    }

    @Test
    @DisplayName("updateInventory_이미처리된조합의재요청일때_재적용하지않고저장된스냅샷을그대로반환한다")
    void updateInventory_재요청_스냅샷그대로반환() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory inventory = inventoryWithId(inventoryId, product, 999);
        // 재요청은 최초 요청과 다른 quantity(30)로 왔다고 가정 - 최초 처리 때 실제 적용된 값은 50이었음
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 30, "DECREASE");
        LocalDateTime snapshotCreatedAt = LocalDateTime.now().minusMinutes(1);
        InventoryIdempotencyKey snapshot = mock(InventoryIdempotencyKey.class);
        when(snapshot.getBeforeQuantity()).thenReturn(100);
        when(snapshot.getAfterQuantity()).thenReturn(50);
        when(snapshot.getCreatedAt()).thenReturn(snapshotCreatedAt);

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)).thenReturn(Optional.of(inventory));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("DECREASE")))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOrderIdAndProductIdAndOperation(
                reqDto.getOrderId(), product.getId(), InventoryChangeType.DECREASE))
                .thenReturn(Optional.of(snapshot));

        ResUpdateInventoryDto result = inventoryService.updateInventory(inventoryId, reqDto);

        assertThat(result.getBeforeQuantity()).isEqualTo(100);
        assertThat(result.getAfterQuantity()).isEqualTo(50);
        // changedQuantity는 저장된 값이 아니라 이번 요청의 quantity(30) 그대로
        assertThat(result.getChangedQuantity()).isEqualTo(30);
        assertThat(result.getUpdatedAt()).isEqualTo(snapshotCreatedAt);

        verify(inventoryRepository, never()).decreaseQuantity(any(), anyInt());
        verify(inventoryRepository, never()).increaseQuantity(any(), anyInt());
        verify(idempotencyKeyRepository, never()).fillSnapshot(any(), any(), any());
    }

    @Test
    @DisplayName("updateInventory_재요청인데스냅샷을찾을수없는극단적인경우_예외발생")
    void updateInventory_재요청_스냅샷없음_예외발생() {
        Product product = productWithId();
        UUID inventoryId = UUID.randomUUID();
        Inventory inventory = inventoryWithId(inventoryId, product, 999);
        ReqUpdateInventoryDto reqDto = updateReqDto(UUID.randomUUID(), 30, "DECREASE");

        when(inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)).thenReturn(Optional.of(inventory));
        when(idempotencyKeyRepository.tryInsert(any(UUID.class), eq(reqDto.getOrderId()), eq(product.getId()), eq("DECREASE")))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOrderIdAndProductIdAndOperation(
                reqDto.getOrderId(), product.getId(), InventoryChangeType.DECREASE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateInventory(inventoryId, reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    private ReqUpdateInventoryDto updateReqDto(UUID orderId, Integer quantity, String type) {
        return ReqUpdateInventoryDto.builder()
                .orderId(orderId)
                .quantity(quantity)
                .type(type)
                .build();
    }

    private Inventory inventoryWithId(UUID id, Product product, Integer quantity) {
        Inventory inventory = Inventory.create(product, UUID.randomUUID(), quantity);
        ReflectionTestUtils.setField(inventory, "id", id);
        return inventory;
    }

    private Product productWithId() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    private DataIntegrityViolationException duplicateKeyException(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"" + constraintName + "\""),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate key", cause);
    }
}