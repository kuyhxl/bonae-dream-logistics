-- GET /api/slack-messages 목록·검색의 기본 정렬(createdAt DESC) 페이징에 사용한다.
-- 필터가 모두 비어 있는 첫 페이지 조회가 가장 빈번하다.
CREATE INDEX idx_p_slack_messages_created_at
    ON message_service.p_slack_messages (created_at DESC)
    WHERE deleted_at IS NULL;

-- 목록·검색 필터 중 선택도가 높은 receiverSlackId 조건에 사용한다.
-- send_status(3종), source_type(2종)은 선택도가 낮아 단독 인덱스를 두지 않는다.
CREATE INDEX idx_p_slack_messages_receiver
    ON message_service.p_slack_messages (receiver_slack_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- 발송 실패 메시지의 오류 이력을 source_id로 역추적한다.
-- append-only 로그이므로 쓰기 비용을 고려해 추적용 인덱스만 유지한다.
CREATE INDEX idx_p_message_error_logs_source
    ON message_service.p_message_error_logs (source_id);
