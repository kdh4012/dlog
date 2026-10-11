-- 고정지출에서 매월 결제일·결제 수단 제거. 고정지출은 매월 1일 날짜로 반영된다
ALTER TABLE fixed_expense DROP COLUMN day_of_month;
ALTER TABLE fixed_expense DROP COLUMN payment_method;

-- 고정지출에서 반영된 지출은 결제 수단이 없을 수 있다
ALTER TABLE expense ALTER COLUMN payment_method DROP NOT NULL;
