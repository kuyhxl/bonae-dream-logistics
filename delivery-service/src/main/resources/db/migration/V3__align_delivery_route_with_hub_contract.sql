ALTER TABLE p_delivery_routes
    DROP CONSTRAINT ck_p_delivery_routes_expected_distance,
    DROP CONSTRAINT ck_p_delivery_routes_expected_duration;

ALTER TABLE p_delivery_routes
    DROP COLUMN expected_distance_km,
    DROP COLUMN expected_duration_min;

ALTER TABLE p_delivery_routes
    ADD COLUMN distance_meters integer NULL,
    ADD COLUMN duration_seconds integer NULL,
    ADD COLUMN distance_km decimal(10, 2) NULL,
    ADD COLUMN duration_min integer NULL;

ALTER TABLE p_delivery_routes
    ADD CONSTRAINT ck_p_delivery_routes_distance_meters
        CHECK (distance_meters IS NULL OR distance_meters >= 0),
    ADD CONSTRAINT ck_p_delivery_routes_duration_seconds
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    ADD CONSTRAINT ck_p_delivery_routes_distance_km
        CHECK (distance_km IS NULL OR distance_km >= 0),
    ADD CONSTRAINT ck_p_delivery_routes_duration_min
        CHECK (duration_min IS NULL OR duration_min >= 0);

CREATE UNIQUE INDEX uk_p_deliveries_active_order
    ON p_deliveries (order_id)
    WHERE deleted_at IS NULL;
