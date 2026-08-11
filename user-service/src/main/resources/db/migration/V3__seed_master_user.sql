-- V3__seed_master_user.sql
-- 마스터 관리자 계정. 가입 API로는 생성할 수 없으므로(권한 상승 차단) 기준 데이터로 직접 INSERT 한다.
-- MASTER는 소속 개념이 없어 hub_id / company_id는 NULL이지만,
-- affiliation_name은 일반 가입 경로에서 필수 값이라 NOT NULL을 유지하고 '본사'를 고정값으로 넣는다.
-- password는 환경변수 MASTER_PASSWORD_HASH의 BCrypt 해시를 주입한다.
-- 로컬 기본값은 .env.template 참조. 운영은 배포 환경변수로 별도 주입한다.

INSERT INTO user_service.p_users
(id, username, password, name, slack_id, role, status,
 affiliation_name, hub_id, company_id,
 approved_by, approved_at, created_at, created_by)
VALUES ('1a3f6d02-0b6e-4f9d-9c1a-5e7f2b8c4d10',
        'master01',
        '${master-password-hash}',
        '마스터관리자',
        'U000MASTER',
        'MASTER',
        'APPROVED',
        '본사',
        NULL,
        NULL,
        'SYSTEM',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'SYSTEM')
    ON CONFLICT (username) DO NOTHING;