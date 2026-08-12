package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.presentation.dto.response.ResSearchInventoryInternalDto;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    // ux_p_inventories_product_hub_active: (product_id, hub_id) where deleted_at is null 부분 유니크 인덱스
    private static final String INVENTORY_PRODUCT_HUB_UNIQUE_CONSTRAINT = "ux_p_inventories_product_hub_active";

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    //상품ID/허브ID로 재고를 검색한다(내부전용). 두 조건 모두 선택값이며, 삭제된 재고는 결과에서 제외한다.
    public PageResponseDto<ResSearchInventoryInternalDto> searchInventories(PageRequestDto pageRequestDto, UUID productId, UUID hubId) {
        Page<Inventory> inventories = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                productId, hubId, pageRequestDto.toPageable());
        return PageResponseDto.from(inventories, ResSearchInventoryInternalDto::from);
    }

    // 상품 생성과 함께 초기 재고를 만든다. 호출 측(ProductService)이 이미 시작한 트랜잭션 안에서 실행되어야 한다.
    // MANDATORY로 강제해 트랜잭션 없이 호출되면 재고가 독립적으로 커밋되지 않고 즉시 IllegalTransactionStateException으로 실패하도록 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public Inventory createInventory(Product product, UUID hubId, Integer quantity) {
        if (inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId)) {
            throw new BusinessException(ErrorCode.INVENTORY_DUPLICATED);
        }

        Inventory inventory = Inventory.create(product, hubId, quantity);

        // 최종 방어선은 DB 부분 유니크 인덱스(product_id, hub_id where deleted_at is null)이며,
        // 위반 시 saveAndFlush에서 예외가 발생하므로 중복 에러로 변환한다.
        try {
            return inventoryRepository.saveAndFlush(inventory);
        } catch (DataIntegrityViolationException e) {
            if (isInventoryProductHubUniqueViolation(e)) {
                throw new BusinessException(ErrorCode.INVENTORY_DUPLICATED);
            }
            throw e;
        }
    }

    //ux_p_inventories_product_hub_active 부분 유니크 인덱스 제약조건 위반 여부 확인 메서드
    private boolean isInventoryProductHubUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && INVENTORY_PRODUCT_HUB_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName());
    }
}