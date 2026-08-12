package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.InventoryChangeType;
import com.bonae.logistics.company.domain.entity.InventoryIdempotencyKey;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.InventoryIdempotencyKeyRepository;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateInventoryDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateInventoryDto;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    // ux_p_inventories_product_hub_active: (product_id, hub_id) where deleted_at is null 부분 유니크 인덱스
    private static final String INVENTORY_PRODUCT_HUB_UNIQUE_CONSTRAINT = "ux_p_inventories_product_hub_active";
    private static final int MIN_CHANGE_QUANTITY = 1;

    private final InventoryRepository inventoryRepository;
    private final InventoryIdempotencyKeyRepository idempotencyKeyRepository;

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

    //<재고 차감(DECREASE)/복구(RESTORE)(내부전용, 주문 서비스 호출) 메서드>
    // 주문 상태에 따라 이 API를 호출할지 여부는 주문 서비스가 판단하므로, 이쪽에서는 주문 상태를 별도로 검증하지 않음.
    // 같은 (orderId, productId, type) 조합의 재요청은 멱등성 키 테이블(p_inventory_idempotency_keys)의 유니크 인덱스로 걸러낸다.
    // 재요청에는 재고를 다시 조회해 역산하지 않고, 최초 처리 시점에 저장해둔 스냅샷을 그대로 반환함
    // (그 사이 다른 주문이 같은 재고를 건드렸어도 "이 주문이 실제로 한 일"은 항상 일관되게 응답하기 위함)
    @Transactional
    public ResUpdateInventoryDto updateInventory(UUID inventoryId, ReqUpdateInventoryDto reqDto) {
        InventoryChangeType type = parseInventoryChangeType(reqDto.getType());
        validateChangeQuantity(reqDto.getQuantity());

        Inventory inventory = inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        UUID productId = inventory.getProduct().getId();

        // 멱등성 키 선점 시도. 이 시점엔 아직 반영 결과(before/after)를 모르므로 선점만 한다.
        // 유니크 인덱스(order_id, product_id, operation) 덕분에 이미 처리된 조합이면 예외 없이 0행 삽입으로 끝남.
        UUID keyId = UUID.randomUUID();
        int keyInserted = idempotencyKeyRepository.tryInsert(keyId, reqDto.getOrderId(), productId, type.name());

        if (keyInserted == 0) { //이미 같은 조합의 요청이 들어와 처리된 경우 -> 그때 저장해둔 스냅샷을 그대로 반환
            return buildReplayResponse(inventoryId, reqDto, type, productId);
        }

        return applyChangeAndFillSnapshot(keyId, inventoryId, reqDto, type);
    }

    // 최초 요청 시: 원자적 조건부 UPDATE로 재고를 반영하고, 그 결과(before/after)를 멱등성 키 row에 채워 넣는다.
    // 재반영 직후 같은 트랜잭션 안에서 조회하므로(UPDATE가 잡은 행 잠금이 커밋 전까지 유지됨) 다른 트랜잭션이
    // 끼어들 수 없어 afterQuantity는 항상 정확하고, beforeQuantity는 거기서 이번 요청의 quantity만큼 역산해도 정확하다.
    private ResUpdateInventoryDto applyChangeAndFillSnapshot(UUID keyId, UUID inventoryId,
                                                               ReqUpdateInventoryDto reqDto, InventoryChangeType type) {
        applyAtomicQuantityChange(inventoryId, type, reqDto.getQuantity());

        Inventory updated = inventoryRepository.findByIdAndDeletedAtIsNull(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        int afterQuantity = updated.getQuantity();
        int beforeQuantity = type == InventoryChangeType.DECREASE
                ? afterQuantity + reqDto.getQuantity()
                : afterQuantity - reqDto.getQuantity();

        idempotencyKeyRepository.fillSnapshot(keyId, beforeQuantity, afterQuantity);

        return ResUpdateInventoryDto.of(reqDto.getOrderId(), inventoryId, beforeQuantity,
                reqDto.getQuantity(), afterQuantity, type.name(), updated.getUpdatedAt());
    }

    // 원자적 조건부 UPDATE로 재고 수량을 반영한다. 락 없이 WHERE 절의 조건으로 동시성을 보장한다.
    private void applyAtomicQuantityChange(UUID inventoryId, InventoryChangeType type, Integer quantity) {
        int updatedRows = switch (type) {
            case DECREASE -> inventoryRepository.decreaseQuantity(inventoryId, quantity);
            case RESTORE -> inventoryRepository.increaseQuantity(inventoryId, quantity);
        };

        if (updatedRows == 0) {
            // DECREASE: WHERE 절의 quantity >= :quantity 조건을 만족하지 못함 = 재고 부족
            // RESTORE: 조회 시점 이후 재고가 삭제된 극단적인 경쟁 상황
            throw new BusinessException(
                    type == InventoryChangeType.DECREASE ? ErrorCode.STOCK_SHORTAGE : ErrorCode.INVENTORY_NOT_FOUND);
        }
    }

    //재고 차감/복구 결과를 응답DTO로 만들어주는 역할(1.방금 실제로 재고를 바꾼 직후 OR 2.이미 처리된 요청 재시도 경우에)
    // 재요청: 재고를 다시 조회하지 않고, before/after는 최초 처리 시점에 저장해둔 스냅샷을 그대로 돌려준다.
    // changedQuantity는 저장해두지 않으므로 이번 요청의 quantity를 그대로 응답에 반영한다.
    private ResUpdateInventoryDto buildReplayResponse(UUID inventoryId, ReqUpdateInventoryDto reqDto,
                                                        InventoryChangeType type, UUID productId) {
        InventoryIdempotencyKey snapshot = idempotencyKeyRepository
                .findByOrderIdAndProductIdAndOperation(reqDto.getOrderId(), productId, type)
                // 방금 tryInsert가 0을 반환해 이미 존재한다고 확인했으므로 여기서 못 찾는 경우는 없어야 한다.
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));

        return ResUpdateInventoryDto.of(reqDto.getOrderId(), inventoryId, snapshot.getBeforeQuantity(),
                reqDto.getQuantity(), snapshot.getAfterQuantity(), type.name(), snapshot.getCreatedAt());
    }

    // 변경 수량(quantity)이 올바른 범위인지 검사하는 검증 메서드. null이거나 1보다 작으면 잘못된 수량으로 처리
    private void validateChangeQuantity(Integer quantity) {
        if (quantity == null || quantity < MIN_CHANGE_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
    }

    private InventoryChangeType parseInventoryChangeType(String type) {
        try {
            return InventoryChangeType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_TYPE);
        }
    }
}