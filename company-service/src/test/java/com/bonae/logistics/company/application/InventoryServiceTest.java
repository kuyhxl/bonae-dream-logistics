package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.presentation.dto.response.ResSearchInventoryInternalDto;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("searchInventories_조건이없을때_전체재고를조회한다")
    void searchInventories_조건이없을때_전체재고를조회한다() {
        Product product = productWithId();
        Inventory inventory = inventoryWithId(product, UUID.randomUUID(), 100);
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Inventory> inventoryPage = new PageImpl<>(List.of(inventory), pageRequestDto.toPageable(), 1);

        when(inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class))).thenReturn(inventoryPage);

        PageResponseDto<ResSearchInventoryInternalDto> resDto =
                inventoryService.searchInventories(pageRequestDto, null, null);

        assertThat(resDto.getContent()).hasSize(1);
        assertThat(resDto.getContent().get(0).getProductId()).isEqualTo(product.getId());
        assertThat(resDto.getContent().get(0).getQuantity()).isEqualTo(100);
        verify(inventoryRepository).searchByProductIdAndHubIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchInventories_productId로검색시_해당조건으로조회한다")
    void searchInventories_productId로검색시_해당조건으로조회한다() {
        Product product = productWithId();
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Inventory> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                eq(product.getId()), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        inventoryService.searchInventories(pageRequestDto, product.getId(), null);

        verify(inventoryRepository).searchByProductIdAndHubIdAndDeletedAtIsNull(
                eq(product.getId()), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchInventories_hubId로검색시_해당조건으로조회한다")
    void searchInventories_hubId로검색시_해당조건으로조회한다() {
        UUID hubId = UUID.randomUUID();
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Inventory> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                isNull(), eq(hubId), any(Pageable.class))).thenReturn(emptyPage);

        inventoryService.searchInventories(pageRequestDto, null, hubId);

        verify(inventoryRepository).searchByProductIdAndHubIdAndDeletedAtIsNull(
                isNull(), eq(hubId), any(Pageable.class));
    }

    @Test
    @DisplayName("searchInventories_productId와hubId모두있을때_두조건으로조회한다")
    void searchInventories_productId와hubId모두있을때_두조건으로조회한다() {
        Product product = productWithId();
        UUID hubId = UUID.randomUUID();
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Inventory> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                eq(product.getId()), eq(hubId), any(Pageable.class))).thenReturn(emptyPage);

        inventoryService.searchInventories(pageRequestDto, product.getId(), hubId);

        verify(inventoryRepository).searchByProductIdAndHubIdAndDeletedAtIsNull(
                eq(product.getId()), eq(hubId), any(Pageable.class));
    }

    @Test
    @DisplayName("searchInventories_정렬기준이허용되지않을때_예외발생")
    void searchInventories_정렬기준이허용되지않을때_예외발생() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        pageRequestDto.setSort("invalidField");

        assertThatThrownBy(() -> inventoryService.searchInventories(pageRequestDto, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SORT_FIELD);

        verify(inventoryRepository, never())
                .searchByProductIdAndHubIdAndDeletedAtIsNull(any(), any(), any());
    }

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

    private Product productWithId() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    private Inventory inventoryWithId(Product product, UUID hubId, Integer quantity) {
        Inventory inventory = Inventory.create(product, hubId, quantity);
        ReflectionTestUtils.setField(inventory, "id", UUID.randomUUID());
        return inventory;
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