CREATE UNIQUE INDEX uk_p_delivery_routes_active_delivery_sequence
    ON p_delivery_routes (delivery_id, sequence_no)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_p_delivery_assignments_active_delivery_sequence
    ON p_delivery_assignments (delivery_id, sequence_no)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_p_delivery_managers_active_hub_delivery_sequence
    ON p_delivery_managers (delivery_sequence)
    WHERE deleted_at IS NULL
      AND manager_type = 'HUB_DELIVERY';

CREATE UNIQUE INDEX uk_p_delivery_managers_active_company_delivery_sequence
    ON p_delivery_managers (hub_id, delivery_sequence)
    WHERE deleted_at IS NULL
      AND manager_type = 'COMPANY_DELIVERY';
