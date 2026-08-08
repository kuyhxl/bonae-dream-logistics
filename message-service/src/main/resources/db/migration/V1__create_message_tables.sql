-- Slack 발송 메시지를 관리한다.
-- 발송 실패 시 재시도 대상이 되므로 상태와 재시도 횟수를 함께 보관한다.
CREATE TABLE message_service.p_slack_messages
(
    id                uuid        NOT NULL,
    receiver_slack_id varchar(100) NOT NULL,
    message           text        NOT NULL,
    send_status       varchar(20) NOT NULL DEFAULT 'PENDING',
    retry_count       integer     NOT NULL DEFAULT 0,
    sent_at           timestamp   NULL,
    source_type       varchar(20) NULL,
    created_at        timestamp   NOT NULL,
    created_by        varchar(10) NOT NULL,
    updated_at        timestamp   NULL,
    updated_by        varchar(10) NULL,
    deleted_at        timestamp   NULL,
    deleted_by        varchar(10) NULL,

    CONSTRAINT pk_p_slack_messages
        PRIMARY KEY (id),

    CONSTRAINT ck_p_slack_messages_send_status
        CHECK (send_status IN ('PENDING', 'SUCCESS', 'FAILED')),

    CONSTRAINT ck_p_slack_messages_source_type
        CHECK (source_type IS NULL OR source_type IN ('USER', 'SYSTEM')),

    CONSTRAINT ck_p_slack_messages_retry_count
        CHECK (retry_count >= 0),

    -- 발송 완료 시점은 성공한 메시지에만 존재한다.
    CONSTRAINT ck_p_slack_messages_sent_at
        CHECK (
            (send_status = 'SUCCESS' AND sent_at IS NOT NULL)
                OR
            (send_status <> 'SUCCESS' AND sent_at IS NULL)
            )
);

-- 주문 정보를 기반으로 AI가 산출한 최종 발송 시한 요청/응답 이력을 관리한다.
CREATE TABLE message_service.p_ai_dispatch_logs
(
    id                     uuid        NOT NULL,
    order_id               uuid        NOT NULL,
    request_content        text        NOT NULL,
    response_content       text        NOT NULL,
    final_dispatch_deadline timestamp  NOT NULL,
    slack_message_id       uuid        NULL,
    created_at             timestamp   NOT NULL,
    created_by             varchar(10) NOT NULL,
    updated_at             timestamp   NULL,
    updated_by             varchar(10) NULL,
    deleted_at             timestamp   NULL,
    deleted_by             varchar(10) NULL,

    CONSTRAINT pk_p_ai_dispatch_logs
        PRIMARY KEY (id),

    -- AI 산출 결과는 같은 메시지 서비스의 Slack 메시지를 참조한다.
    CONSTRAINT fk_p_ai_dispatch_logs_slack_message
        FOREIGN KEY (slack_message_id)
            REFERENCES message_service.p_slack_messages (id)
            ON DELETE RESTRICT
);

-- Slack 발송 및 AI 생성 과정에서 발생한 오류 이력을 관리한다.
-- 장애 분석용 append-only 테이블이므로 수정/논리 삭제 컬럼을 두지 않는다.
CREATE TABLE message_service.p_message_error_logs
(
    id            uuid        NOT NULL,
    error_type    varchar(20) NOT NULL,
    source_id     uuid        NOT NULL,
    attempt_no    integer     NOT NULL DEFAULT 1,
    error_code    varchar(50) NOT NULL,
    error_message text        NOT NULL,
    trace_id      varchar(50) NULL,
    created_at    timestamp   NOT NULL,

    CONSTRAINT pk_p_message_error_logs
        PRIMARY KEY (id),

    CONSTRAINT ck_p_message_error_logs_error_type
        CHECK (error_type IN ('SLACK_SEND', 'AI_GENERATION')),

    CONSTRAINT ck_p_message_error_logs_attempt_no
        CHECK (attempt_no >= 1)
);
