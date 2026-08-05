-- 전국 물류 허브의 기본 정보를 관리한다.
CREATE TABLE hub_service.p_hubs
(
    id          uuid             NOT NULL,
    name        varchar(100)     NOT NULL,
    address     varchar(255)     NOT NULL,
    latitude    double precision NOT NULL,
    longitude   double precision NOT NULL,
    created_at  timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  varchar(10)      NOT NULL,
    updated_at  timestamp        NULL,
    updated_by  varchar(10)      NULL,
    deleted_at  timestamp        NULL,
    deleted_by  varchar(10)      NULL,

    CONSTRAINT pk_p_hubs
        PRIMARY KEY (id),

    -- 허브 좌표의 유효 범위를 DB 레벨에서 보장한다.
    CONSTRAINT ck_p_hubs_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT ck_p_hubs_longitude
        CHECK (longitude BETWEEN -180 AND 180)
);

-- Relay 경로 탐색에 사용하는 허브 간 직접 연결 간선을 관리한다.
CREATE TABLE hub_service.p_hub_routes
(
    id                  uuid        NOT NULL,
    departure_hub_id    uuid        NOT NULL,
    arrival_hub_id      uuid        NOT NULL,
    distance_meters     integer     NOT NULL,
    duration_seconds    integer     NOT NULL,
    created_at          timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          varchar(10) NOT NULL,
    updated_at          timestamp   NULL,
    updated_by          varchar(10) NULL,
    deleted_at          timestamp   NULL,
    deleted_by          varchar(10) NULL,

    CONSTRAINT pk_p_hub_routes
        PRIMARY KEY (id),

    -- 허브 이동정보는 같은 허브 서비스의 허브 데이터를 참조한다.
    CONSTRAINT fk_p_hub_routes_departure_hub
        FOREIGN KEY (departure_hub_id)
            REFERENCES hub_service.p_hubs (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_p_hub_routes_arrival_hub
        FOREIGN KEY (arrival_hub_id)
            REFERENCES hub_service.p_hubs (id)
            ON DELETE RESTRICT,

    -- 경로 탐색에 사용할 수 없는 간선 데이터의 저장을 방지한다.
    CONSTRAINT ck_p_hub_routes_different_hubs
        CHECK (departure_hub_id <> arrival_hub_id),

    CONSTRAINT ck_p_hub_routes_positive_distance
        CHECK (distance_meters > 0),

    CONSTRAINT ck_p_hub_routes_positive_duration
        CHECK (duration_seconds > 0)
);