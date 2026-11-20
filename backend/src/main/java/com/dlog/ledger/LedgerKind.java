package com.dlog.ledger;

/** 가계부 항목 구분 */
public enum LedgerKind {
    /** 매월 들어오는 수입 (월급, 배당 등) */
    INCOME,
    /** 매월 나가는 고정지출 (월세, 통신비, 구독료 등) */
    FIXED_EXPENSE,
    /** 부채 잔액 (대출 등) */
    DEBT
}
