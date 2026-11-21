-- 목표 달성 여부와 표시 순서
ALTER TABLE goal ADD COLUMN done BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE goal ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
-- 기존 목표는 등록한 순서 그대로
UPDATE goal SET sort_order = id;
