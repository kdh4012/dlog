package com.dlog.expense.dto;

import com.dlog.expense.FixedExpense;

import java.time.YearMonth;

/** active: 이번 달 이후에도 반영되는지 (종료 월이 지났으면 false) */
public record FixedExpenseResponse(
        Long id,
        String name,
        long amount,
        YearMonth startMonth,
        YearMonth endMonth,
        String memo,
        boolean active
) {
    public static FixedExpenseResponse from(FixedExpense f, YearMonth thisMonth) {
        return new FixedExpenseResponse(f.getId(), f.getName(), f.getAmount(),
                f.startYearMonth(), f.endYearMonth(), f.getMemo(),
                f.endYearMonth() == null || !f.endYearMonth().isBefore(thisMonth));
    }
}
