package com.dlog.ledger.dto;

import java.util.List;

/**
 * 한 달의 가계부. 항목은 구분별로 금액 큰 순.
 *
 * @param month             yyyy-MM
 * @param incomeTotal       그 달 수입 합계
 * @param fixedExpenseTotal 그 달 고정지출 합계
 * @param monthlyBalance    수입 - 고정지출 (음수면 적자)
 * @param debtTotal         현재 부채 잔액 합계 (달과 무관)
 * @param trend             그 달까지 최근 12개월 (오래된 달부터)
 */
public record LedgerResponse(
        String month,
        long incomeTotal,
        long fixedExpenseTotal,
        long monthlyBalance,
        long debtTotal,
        List<LedgerItemResponse> incomes,
        List<LedgerItemResponse> fixedExpenses,
        List<LedgerItemResponse> debts,
        List<MonthTotal> trend
) {
    public record MonthTotal(String month, long income, long fixedExpense) {
    }
}
