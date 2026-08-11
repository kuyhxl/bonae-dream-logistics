package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    // ux_p_inventories_product_hub_active: (product_id, hub_id) where deleted_at is null 부분 유니크 인덱스
    private static final String INVENTORY_PRODUCT_HUB_UNIQUE_CONSTRAINT = "ux_p_inventories_product_hub_active";

    private final InventoryRepository inventoryRepository;

    // 상품 생성과 함께 초기 재고를 만든다. 호출 측(ProductService)이 이미 시작한 트랜잭션 안에서 실행되어야 한다.
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