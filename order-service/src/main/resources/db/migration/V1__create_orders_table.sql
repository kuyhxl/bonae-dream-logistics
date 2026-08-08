CREATE TABLE p_orders (
                          id                    UUID PRIMARY KEY,
                          requester_company_id  UUID NOT NULL,
                          receiver_company_id   UUID NOT NULL,
                          product_id            UUID NOT NULL,
                          quantity              INT NOT NULL CHECK (quantity > 0),
                          unit_price            DECIMAL(12,2) NOT NULL,
                          total_price           DECIMAL(14,2) NOT NULL,
                          due_date              TIMESTAMP NOT NULL,
                          remarks               VARCHAR(500),
                          status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          created_at            TIMESTAMP NOT NULL,
                          created_by            VARCHAR(50) NOT NULL,
                          updated_at            TIMESTAMP,
                          updated_by            VARCHAR(50),
                          deleted_at            TIMESTAMP,
                          deleted_by            VARCHAR(50)
);