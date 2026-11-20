package com.dlog.ledger.dto;

import java.util.List;

/**
 * 가계부 전체. 항목은 구분별로 금액 큰 순.
 *
 * @param incomeTotal       월 수입 합계
 * @param fixedExpenseTotal 월 고정지출 합계
 * @param monthlyBalance    월 수입 - 월 고정지출 (음수면 적자)
 * @param debtTotal         부채 잔액 합계
 */
public record LedgerResponse(
        long incomeTotal,
        long fixedExpenseTotal,
        long monthlyBalance,
        long debtTotal,
        List<LedgerItemResponse> incomes,
        List<LedgerItemResponse> fixedExpenses,
        List<LedgerItemResponse> debts
) {
}
