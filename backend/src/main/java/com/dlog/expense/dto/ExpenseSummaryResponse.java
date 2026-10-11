package com.dlog.expense.dto;

import com.dlog.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 월 지출 요약.
 *
 * @param total          해당 월 합계
 * @param previousTotal  전월 합계
 * @param changeRate     전월 대비 증감률(%). 전월 0 이면 null
 * @param budget         적용 예산 (해당 월 또는 이어받은 이전 달 예산). 없으면 null
 * @param budgetMonth    예산이 설정된 달 (이어받은 경우 이전 달)
 * @param budgetUsedRate 예산 사용률(%). 예산 없으면 null
 * @param byCategory     카테고리별 합계 (금액 큰 순)
 * @param trend          최근 6개월 월별 합계 (오래된 순, 해당 월 포함)
 */
public record ExpenseSummaryResponse(
        YearMonth month,
        long total,
        int count,
        long previousTotal,
        BigDecimal changeRate,
        Long budget,
        YearMonth budgetMonth,
        BigDecimal budgetUsedRate,
        List<CategorySummary> byCategory,
        List<MonthTotal> trend
) {
    /** weight: 해당 월 합계 중 비중(%) */
    public record CategorySummary(ExpenseCategory category, long amount, int count, BigDecimal weight) {
    }

    public record MonthTotal(YearMonth month, long total) {
    }
}
