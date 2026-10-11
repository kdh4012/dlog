package com.dlog.expense.dto;

import com.dlog.expense.ExpenseCategory;
import com.dlog.expense.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull LocalDate spentOn,
        @NotNull @Positive @Max(1_000_000_000_000L) Long amount,
        @NotNull ExpenseCategory category,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 100) String merchant,
        @Size(max = 500) String memo
) {
}
