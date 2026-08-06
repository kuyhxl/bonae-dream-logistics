CREATE TABLE p_deliveries
(
    id                  uuid         NOT NULL,
    order_id            uuid         NOT NULL,
    origin_hub_id       uuid         NOT NULL,
    destination_hub_id  uuid         NOT NULL,
    receiver_company_id uuid         NOT NULL,
    delivery_manager_id uuid         NULL,
    receiver_name       varchar(50)  NOT NULL,
    receiver_slack_id   varchar(50)  NOT NULL,
    delivery_address    varchar(255) NOT NULL,
    status              varchar(30)  NOT NULL,
    assigned_at         timestamp    NULL,
    started_at          timestamp    NULL,
    completed_at        timestamp    NULL,
    created_at          timestamp    NOT NULL,
    created_by          varchar(10)  NOT NULL,
    updated_at          timestamp    NULL,
    updated_by          varchar(10)  NULL,
    deleted_at          timestamp    NULL,
    deleted_by          varchar(10)  NULL,

    CONSTRAINT pk_p_deliveries
        PRIMARY KEY (id),

    CONSTRAINT ck_p_deliveries_status
        CHECK (status IN ('READY', 'HUB_WAITING', 'HUB_MOVING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'))
);

CREATE TABLE p_delivery_routes
(
    id                    uuid           NOT NULL,
    delivery_id           uuid           NOT NULL,
    sequence_no           integer        NOT NULL,
    from_hub_id           uuid           NOT NULL,
    to_hub_id             uuid           NOT NULL,
    delivery_manager_id   uuid           NULL,
    route_status          varchar(30)    NOT NULL,
    expected_distance_km  decimal(10, 2) NULL,
    actual_distance_km    decimal(10, 2) NULL,
    expected_duration_min integer        NULL,
    actual_duration_min   integer        NULL,
    actual_departed_at    timestamp      NULL,
    actual_arrived_at     timestamp      NULL,
    created_at            timestamp      NOT NULL,
    created_by            varchar(10)    NOT NULL,
    updated_at            timestamp      NULL,
    updated_by            varchar(10)    NULL,
    deleted_at            timestamp      NULL,
    deleted_by            varchar(10)    NULL,

    CONSTRAINT pk_p_delivery_routes
        PRIMARY KEY (id),

    CONSTRAINT ck_p_delivery_routes_sequence_no
        CHECK (sequence_no >= 1),

    CONSTRAINT ck_p_delivery_routes_status
        CHECK (route_status IN ('WAITING', 'IN_TRANSIT', 'ARRIVED')),

    CONSTRAINT ck_p_delivery_routes_expected_distance
        CHECK (expected_distance_km IS NULL OR expected_distance_km >= 0),

    CONSTRAINT ck_p_delivery_routes_actual_distance
        CHECK (actual_distance_km IS NULL OR actual_distance_km >= 0),

    CONSTRAINT ck_p_delivery_routes_expected_duration
        CHECK (expected_duration_min IS NULL OR expected_duration_min >= 0),

    CONSTRAINT ck_p_delivery_routes_actual_duration
        CHECK (actual_duration_min IS NULL OR actual_duration_min >= 0)
);

CREATE TABLE p_delivery_assignments
(
    id                  uuid         NOT NULL,
    delivery_id         uuid         NOT NULL,
    delivery_manager_id uuid         NOT NULL,
    sequence_no         integer      NOT NULL,
    assignment_status   varchar(30)  NOT NULL,
    assigned_at         timestamp    NOT NULL,
    unassigned_at       timestamp    NULL,
    reason              varchar(255) NULL,
    created_at          timestamp    NOT NULL,
    created_by          varchar(10)  NOT NULL,
    updated_at          timestamp    NULL,
    updated_by          varchar(10)  NULL,
    deleted_at          timestamp    NULL,
    deleted_by          varchar(10)  NULL,

    CONSTRAINT pk_p_delivery_assignments
        PRIMARY KEY (id),

    CONSTRAINT ck_p_delivery_assignments_sequence_no
        CHECK (sequence_no >= 1),

    CONSTRAINT ck_p_delivery_assignments_status
        CHECK (assignment_status IN ('ASSIGNED', 'REASSIGNED', 'CANCELLED', 'COMPLETED'))
);

CREATE TABLE p_delivery_managers
(
    id                uuid        NOT NULL,
    hub_id            uuid        NULL,
    manager_type      varchar(20) NOT NULL,
    delivery_sequence integer     NOT NULL,
    created_at        timestamp   NOT NULL,
    created_by        varchar(10) NOT NULL,
    updated_at        timestamp   NULL,
    updated_by        varchar(10) NULL,
    deleted_at        timestamp   NULL,
    deleted_by        varchar(10) NULL,

    CONSTRAINT pk_p_delivery_managers
        PRIMARY KEY (id),

    CONSTRAINT ck_p_delivery_managers_type
        CHECK (manager_type IN ('HUB_DELIVERY', 'COMPANY_DELIVERY')),

    CONSTRAINT ck_p_delivery_managers_sequence
        CHECK (delivery_sequence >= 0),

    CONSTRAINT ck_p_delivery_managers_hub_mapping
        CHECK (
            (manager_type = 'HUB_DELIVERY' AND hub_id IS NULL)
                OR
            (manager_type = 'COMPANY_DELIVERY' AND hub_id IS NOT NULL)
            )
);
