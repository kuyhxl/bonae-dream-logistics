CREATE TABLE p_companies (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    hub_id UUID NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(10),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(10),
    CONSTRAINT pk_p_companies PRIMARY KEY (id)
);

-- 삭제되지 않은(deleted_at IS NULL) 업체 중에서만 name + address 조합이 유일해야 한다.
CREATE UNIQUE INDEX ux_p_companies_name_address_active
    ON p_companies (name, address)
    WHERE deleted_at IS NULL;