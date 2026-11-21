-- 학습 노트: '문제 해결 사례'(TROUBLE)를 '업무·도메인'(DOMAIN)으로 합친다.
-- 기존 업무·도메인 카드 뒤에 오도록 정렬 순서를 그 최댓값만큼 민다.
UPDATE study_card
SET sort_order = sort_order + (SELECT COALESCE(MAX(d.sort_order), 0) FROM study_card d WHERE d.category = 'DOMAIN'),
    category   = 'DOMAIN'
WHERE category = 'TROUBLE';
