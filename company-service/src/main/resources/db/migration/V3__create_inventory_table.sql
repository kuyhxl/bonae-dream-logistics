CREATE TABLE p_inventories (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(10),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(10),
    CONSTRAINT pk_p_inventories PRIMARY KEY (id),
    CONSTRAINT fk_p_inventories_product_id FOREIGN KEY (product_id) REFERENCES p_products (id)
);

-- 삭제되지 않은(deleted_at IS NULL) 재고 중에서만 product_id + hub_id 조합이 유일해야 한다.
CREATE UNIQUE INDEX ux_p_inventories_product_hub_active
    ON p_inventories (product_id, hub_id)
    WHERE deleted_at IS NULL;