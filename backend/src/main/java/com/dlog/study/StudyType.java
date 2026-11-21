package com.dlog.study;

/** 기술·업무 카드의 유형. 한 카드에 여러 개를 붙일 수 있다 (선언 순서 = 화면 표시 순서) */
public enum StudyType {
    /** 담당 업무 */
    WORK,
    /** 도메인 지식 */
    DOMAIN,
    /** 기술 개념 */
    TECH,
    /** 문제 해결 사례 */
    TROUBLE
}
