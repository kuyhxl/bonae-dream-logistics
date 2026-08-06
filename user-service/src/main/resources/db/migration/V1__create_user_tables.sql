-- 사용자 정보를 관리한다.
-- 권한(role)과 소속(hub_id, company_id)은 가입 시점에 NULL이며, 승인 시 관리자가 확정한다.
CREATE TABLE user_service.p_users
(
    id               uuid         NOT NULL,
    username         varchar(10)  NOT NULL,
    password         varchar(100) NOT NULL,
    name             varchar(100) NOT NULL,
    slack_id         varchar(100) NOT NULL,
    role             varchar(20) NULL,
    status           varchar(20) NOT NULL,
    affiliation_name varchar(100) NOT NULL,
    hub_id           uuid         NULL,
    company_id       uuid         NULL,
    approved_by      varchar(10)  NULL,
    approved_at      timestamp    NULL,
    created_at       timestamp    NOT NULL,
    created_by       varchar(100) NOT NULL,
    updated_at       timestamp    NULL,
    updated_by       varchar(100) NULL,
    deleted_at       timestamp    NULL,
    deleted_by       varchar(100) NULL,

    CONSTRAINT pk_p_users
        PRIMARY KEY (id),

    CONSTRAINT uk_p_users_username
        UNIQUE (username)
);