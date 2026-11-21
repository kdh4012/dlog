-- 학습 노트 분류를 '면접 질문'(INTERVIEW)과 '기술·업무'(KNOWLEDGE) 둘로 줄이고,
-- 기술·업무는 유형(types: WORK, DOMAIN, TECH, TROUBLE 쉼표 구분, 여러 개)으로 나눈다.
ALTER TABLE study_card ADD COLUMN types VARCHAR(100) NOT NULL DEFAULT '';

-- 자기소개 → 면접 질문 (자기소개가 앞에 오도록 기존 면접 질문을 그 최댓값만큼 뒤로 민다)
UPDATE study_card
SET sort_order = sort_order + (SELECT COALESCE(MAX(i.sort_order), 0) FROM study_card i WHERE i.category = 'INTRO')
WHERE category = 'INTERVIEW';
UPDATE study_card SET category = 'INTERVIEW' WHERE category = 'INTRO';

-- 기술 개념 → 기술·업무 + TECH (업무·도메인 카드 뒤로)
UPDATE study_card
SET sort_order = sort_order + (SELECT COALESCE(MAX(d.sort_order), 0) FROM study_card d WHERE d.category = 'DOMAIN'),
    types      = 'TECH',
    category   = 'KNOWLEDGE'
WHERE category = 'TECH';

-- 업무·도메인 → 기술·업무 + WORK, DOMAIN (예전 분류가 둘을 함께 뜻했으므로 둘 다 붙인다)
UPDATE study_card SET types = 'WORK,DOMAIN', category = 'KNOWLEDGE' WHERE category = 'DOMAIN';
