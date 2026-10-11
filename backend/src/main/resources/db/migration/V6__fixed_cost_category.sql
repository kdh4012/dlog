-- 고정지출은 카테고리를 따로 고르지 않고 모두 '고정비'(FIXED_COST) 카테고리로 반영한다
UPDATE expense SET category = 'FIXED_COST' WHERE kind = 'FIXED';
ALTER TABLE fixed_expense DROP COLUMN category;
