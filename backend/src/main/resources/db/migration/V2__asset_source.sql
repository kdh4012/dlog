-- 자산 출처. MANUAL: 직접 입력, UPBIT / TOSS: 외부 API 동기화 (수정·삭제 불가, 동기화로만 갱신)
ALTER TABLE asset ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

CREATE INDEX idx_asset_source ON asset (source);
