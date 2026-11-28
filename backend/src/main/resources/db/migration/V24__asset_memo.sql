-- 자산 포트폴리오 메모 (매도·매수 계획 등 글 하나, 관리자만)
CREATE TABLE asset_memo
(
    id         BIGINT        NOT NULL PRIMARY KEY, -- 항상 1
    content    VARCHAR(2000) NOT NULL,
    updated_at TIMESTAMP     NOT NULL
);
