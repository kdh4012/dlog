-- 가계부 수입·고정지출을 달마다 기록 (부채는 월 구분 없이 현재 잔액)
ALTER TABLE ledger_item ADD COLUMN ledger_month VARCHAR(7); -- yyyy-MM, 부채는 null
CREATE INDEX idx_ledger_item_month ON ledger_item (ledger_month);

-- 지금까지의 항목은 이번 달(한국 시간) 기록으로 둔다
UPDATE ledger_item
SET ledger_month = TO_CHAR(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul', 'YYYY-MM')
WHERE kind <> 'DEBT';

-- 항목을 채운(이월했거나 직접 입력한) 달. 여기 있는 달은 항목을 다 지워도 다시 이월하지 않는다
CREATE TABLE ledger_month
(
    ym         VARCHAR(7) NOT NULL PRIMARY KEY, -- yyyy-MM
    created_at TIMESTAMP  NOT NULL
);

INSERT INTO ledger_month (ym, created_at)
SELECT DISTINCT ledger_month, CURRENT_TIMESTAMP
FROM ledger_item
WHERE ledger_month IS NOT NULL;
