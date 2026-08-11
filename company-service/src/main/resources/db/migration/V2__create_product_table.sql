CREATE TABLE p_products (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    company_id UUID NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(10),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(10),
    CONSTRAINT pk_p_products PRIMARY KEY (id),
    CONSTRAINT fk_p_products_company_id FOREIGN KEY (company_id) REFERENCES p_companies (id)
);

-- 삭제되지 않은(deleted_at IS NULL) 상품 중에서만 name + company_id 조합이 유일해야 한다.
CREATE UNIQUE INDEX ux_p_products_name_company_active
    ON p_products (name, company_id)
    WHERE deleted_at IS NULL;