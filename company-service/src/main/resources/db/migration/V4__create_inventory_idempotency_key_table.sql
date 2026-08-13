-- 같은 주문(order_id)의 같은 상품(product_id)에 대한 같은 작업(operation: DECREASE/RESTORE)이 중복 요청되어도 한 번만 처리되도록 막는 멱등성 키 테이블.
CREATE TABLE p_inventory_idempotency_keys (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_p_inventory_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT fk_p_inventory_idempotency_keys_product_id FOREIGN KEY (product_id) REFERENCES p_products (id)
);

CREATE UNIQUE INDEX ux_p_inventory_idempotency_keys_order_product_op
    ON p_inventory_idempotency_keys (order_id, product_id, operation);