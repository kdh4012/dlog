package com.dlog.expense.dto;

import com.dlog.expense.Expense;
import com.dlog.expense.ExpenseCategory;
import com.dlog.expense.ExpenseKind;
import com.dlog.expense.PaymentMethod;

import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        LocalDate spentOn,
        long amount,
        ExpenseCategory category,
        PaymentMethod paymentMethod,
        String merchant,
        String memo,
        ExpenseKind kind,
        Long fixedExpenseId
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.getId(), e.getSpentOn(), e.getAmount(), e.getCategory(), e.getPaymentMethod(),
                e.getMerchant(), e.getMemo(), e.getKind(), e.getFixedExpenseId());
    }
}
