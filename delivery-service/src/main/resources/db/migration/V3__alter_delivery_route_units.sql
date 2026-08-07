ALTER TABLE p_delivery_routes
    RENAME COLUMN expected_distance_km TO distance_meters;

ALTER TABLE p_delivery_routes
    RENAME COLUMN expected_duration_min TO duration_seconds;

ALTER TABLE p_delivery_routes
    RENAME CONSTRAINT ck_p_delivery_routes_expected_distance
        TO ck_p_delivery_routes_distance_meters;

ALTER TABLE p_delivery_routes
    RENAME CONSTRAINT ck_p_delivery_routes_expected_duration
        TO ck_p_delivery_routes_duration_seconds;

ALTER TABLE p_delivery_routes
    ALTER COLUMN distance_meters TYPE integer
        USING CASE
                  WHEN distance_meters IS NULL THEN NULL
                  ELSE ROUND(distance_meters * 1000)::integer
              END;

UPDATE p_delivery_routes
SET duration_seconds = CASE
                           WHEN duration_seconds IS NULL THEN NULL
                           ELSE duration_seconds * 60
                       END;

ALTER TABLE p_delivery_routes
    ADD COLUMN distance_km decimal(10, 2) NULL;

ALTER TABLE p_delivery_routes
    ADD COLUMN duration_min integer NULL;

UPDATE p_delivery_routes
SET distance_km = CASE
                      WHEN distance_meters IS NULL THEN NULL
                      ELSE ROUND(distance_meters / 1000.0, 2)
                  END,
    duration_min = CASE
                       WHEN duration_seconds IS NULL THEN NULL
                       ELSE CEIL(duration_seconds / 60.0)::integer
                   END;

ALTER TABLE p_delivery_routes
    ADD CONSTRAINT ck_p_delivery_routes_distance_km
        CHECK (distance_km IS NULL OR distance_km >= 0),
    ADD CONSTRAINT ck_p_delivery_routes_duration_min
        CHECK (duration_min IS NULL OR duration_min >= 0);

CREATE UNIQUE INDEX uk_p_deliveries_active_order
    ON p_deliveries (order_id)
    WHERE deleted_at IS NULL;
