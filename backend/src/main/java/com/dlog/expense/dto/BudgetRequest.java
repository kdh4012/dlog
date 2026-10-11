package com.dlog.expense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.YearMonth;

/** amount 0 이면 해당 월 예산을 지운다 (이전 달 예산을 이어서 사용) */
public record BudgetRequest(
        @NotNull YearMonth month,
        @NotNull @PositiveOrZero @Max(1_000_000_000_000L) Long amount
) {
}
