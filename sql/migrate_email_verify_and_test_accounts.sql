-- =============================================================
-- 오늘냉장고 — 이메일 인증 컬럼 마이그레이션 + 테스트 계정 생성
-- 실행 대상: PostgreSQL (today_fridge 스키마)
-- 날짜: 2026-04-28
-- =============================================================

-- [1] 신규 컬럼 추가 (이미 존재하면 무시)
-- =============================================================
ALTER TABLE today_fridge.users
    ADD COLUMN IF NOT EXISTS email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verify_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_verify_expiry TIMESTAMPTZ;


-- [2] 테스트 계정 삽입
-- =============================================================
-- 비밀번호: Test1234!  (BCrypt 해시)
-- 아이디 중복 시 무시 (ON CONFLICT)

-- 계정 1: 이메일 인증 완료된 정상 사용자
INSERT INTO today_fridge.users
    (login_id, email, password_hash, nickname, status, email_verified, created_at, updated_at)
VALUES
    ('testuser',
     'testuser@todayfridge.com',
     '$2a$10$rDkPvvAFV6kqVBIeAg4S9OjFGFIVA9V6yy0nI8JfR1xQZgUOmNSoW',
     '테스트유저',
     'ACTIVE',
     TRUE,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP)
ON CONFLICT (login_id) DO NOTHING;

-- 계정 2: 이메일 미인증 사용자 (인증 플로우 테스트용)
INSERT INTO today_fridge.users
    (login_id, email, password_hash, nickname, status,
     email_verified, email_verify_token, email_verify_expiry,
     created_at, updated_at)
VALUES
    ('testuser2',
     'testuser2@todayfridge.com',
     '$2a$10$rDkPvvAFV6kqVBIeAg4S9OjFGFIVA9V6yy0nI8JfR1xQZgUOmNSoW',
     '미인증유저',
     'PENDING_VERIFICATION',
     FALSE,
     'test-verify-token-12345',
     CURRENT_TIMESTAMP + INTERVAL '24 hours',
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP)
ON CONFLICT (login_id) DO NOTHING;


-- [3] 확인 쿼리
-- =============================================================
SELECT user_id, login_id, email, nickname, status, email_verified,
       email_verify_token IS NOT NULL AS has_token
FROM   today_fridge.users
WHERE  login_id IN ('testuser', 'testuser2');
