-- quantity는 요청 시점 값을 응답에만 그대로 반영하기로 하여 별도 저장이 필요 없다.
-- before_quantity/after_quantity(최초 처리 시점 스냅샷)만 저장한다.
ALTER TABLE p_inventory_idempotency_keys DROP COLUMN quantity;
