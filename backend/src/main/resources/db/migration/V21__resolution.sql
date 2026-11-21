-- 홈 화면 맨 위의 '다짐' (한 줄 또는 여러 줄 글 하나, 관리자만)
CREATE TABLE resolution
(
    id         BIGINT       NOT NULL PRIMARY KEY, -- 항상 1
    content    VARCHAR(1000) NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);
