-- 재요청(재시도) 시 "현재 재고 상태"를 다시 조회해 역산하면 그 사이 다른 주문이 같은 재고를 건드렸을 때 부정확한 값이 나올 수 있어서,
-- 최초 처리 시점의 결과를 스냅샷으로 저장해두고 재요청에는 그대로 반환한다.
-- 선점(INSERT) 시점에는 아직 스냅샷 값을 모르므로 NULL 허용으로 두고, 실제 반영 직후 같은 트랜잭션 안에서 채운다.
ALTER TABLE p_inventory_idempotency_keys ADD COLUMN quantity INTEGER;
ALTER TABLE p_inventory_idempotency_keys ADD COLUMN before_quantity INTEGER;
ALTER TABLE p_inventory_idempotency_keys ADD COLUMN after_quantity INTEGER;
