-- 일별 총 자산 기록 (토스·업비트·금 등 전체를 원화로 합친 값). 동기화할 때마다 그날 값을 덮어쓴다
CREATE TABLE asset_snapshot
(
    snap_date  DATE           NOT NULL PRIMARY KEY, -- 한국 시간 기준 날짜
    eval_krw   NUMERIC(24, 2) NOT NULL,             -- 평가금액 합계
    cost_krw   NUMERIC(24, 2) NOT NULL,             -- 매입금액 합계
    updated_at TIMESTAMP      NOT NULL
);
