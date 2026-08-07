ALTER TABLE p_delivery_routes
    RENAME COLUMN expected_distance_km TO distance_km;

ALTER TABLE p_delivery_routes
    RENAME COLUMN expected_duration_min TO duration_min;

ALTER TABLE p_delivery_routes
    RENAME CONSTRAINT ck_p_delivery_routes_expected_distance
        TO ck_p_delivery_routes_distance_km;

ALTER TABLE p_delivery_routes
    RENAME CONSTRAINT ck_p_delivery_routes_expected_duration
        TO ck_p_delivery_routes_duration_min;

ALTER TABLE p_delivery_routes
    ADD COLUMN distance_meters integer NULL;

ALTER TABLE p_delivery_routes
    ADD COLUMN duration_seconds integer NULL;

UPDATE p_delivery_routes
SET distance_meters = CASE
                          WHEN distance_km IS NULL THEN NULL
                          ELSE ROUND(distance_km * 1000)::integer
                      END,
    duration_seconds = CASE
                           WHEN duration_min IS NULL THEN NULL
                           ELSE duration_min * 60
                       END;

ALTER TABLE p_delivery_routes
    ADD CONSTRAINT ck_p_delivery_routes_distance_meters
        CHECK (distance_meters IS NULL OR distance_meters >= 0),
    ADD CONSTRAINT ck_p_delivery_routes_duration_seconds
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0);

CREATE UNIQUE INDEX uk_p_deliveries_active_order
    ON p_deliveries (order_id)
    WHERE deleted_at IS NULL;
