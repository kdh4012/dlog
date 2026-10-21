-- 관심 종목 표시 순서 (작을수록 위). 기존 종목은 담은 순서 그대로
ALTER TABLE watchlist_stock ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
UPDATE watchlist_stock SET sort_order = id;
