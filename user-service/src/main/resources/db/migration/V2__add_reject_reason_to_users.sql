-- 가입 거절 시 관리자가 남기는 사유. 승인 건은 NULL.
ALTER TABLE user_service.p_users
    ADD COLUMN reject_reason varchar(255) NULL;