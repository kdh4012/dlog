package com.dlog.expense.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.YearMonth;

public record FixedExpenseRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive @Max(1_000_000_000_000L) Long amount,
        @NotNull YearMonth startMonth,
        YearMonth endMonth,
        @Size(max = 500) String memo
) {
    @AssertTrue(message = "종료 월은 시작 월 이후여야 합니다.")
    public boolean isValidPeriod() {
        return startMonth == null || endMonth == null || !endMonth.isBefore(startMonth);
    }
}
