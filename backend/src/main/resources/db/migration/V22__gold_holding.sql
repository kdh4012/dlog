-- 금 현물 보유량 (직접 입력). 시세는 공공데이터포털 KRX 금시세로 갱신해 asset(source=GOLD)에 반영한다
CREATE TABLE gold_holding
(
    id         BIGINT         NOT NULL PRIMARY KEY, -- 항상 1
    grams      NUMERIC(18, 4) NOT NULL,             -- 보유량 (g)
    avg_price  NUMERIC(18, 2) NOT NULL,             -- 평균 매입 단가 (원/g)
    last_price NUMERIC(18, 2),                      -- 마지막으로 받은 시세 (원/g)
    price_date DATE,                                -- 그 시세의 기준일
    updated_at TIMESTAMP      NOT NULL
);
