package com.dlog.expense;

import com.dlog.expense.dto.BudgetRequest;
import com.dlog.expense.dto.ExpenseRequest;
import com.dlog.expense.dto.ExpenseResponse;
import com.dlog.expense.dto.ExpenseSummaryResponse;
import com.dlog.expense.dto.ExpenseSummaryResponse.CategorySummary;
import com.dlog.expense.dto.ExpenseSummaryResponse.MonthTotal;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    /** 요약의 월별 추이에 보여줄 개월 수 (해당 월 포함) */
    private static final int TREND_MONTHS = 6;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseRepository expenseRepository;
    private final ExpenseBudgetRepository budgetRepository;

    public List<ExpenseResponse> getMonth(YearMonth month) {
        return expenseRepository
                .findAllBySpentOnGreaterThanEqualAndSpentOnLessThanOrderBySpentOnDescIdDesc(start(month), end(month))
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    public ExpenseSummaryResponse getSummary(YearMonth month) {
        List<Expense> expenses = expenseRepository
                .findAllBySpentOnGreaterThanEqualAndSpentOnLessThanOrderBySpentOnDescIdDesc(start(month), end(month));
        long total = expenses.stream().mapToLong(Expense::getAmount).sum();
        long previousTotal = sumOf(month.minusMonths(1));

        // 카테고리별 합계·건수 (금액 큰 순)
        Map<ExpenseCategory, long[]> agg = new EnumMap<>(ExpenseCategory.class);
        for (Expense e : expenses) {
            long[] v = agg.computeIfAbsent(e.getCategory(), k -> new long[2]);
            v[0] += e.getAmount();
            v[1]++;
        }
        List<CategorySummary> byCategory = agg.entrySet().stream()
                .map(en -> new CategorySummary(en.getKey(), en.getValue()[0], (int) en.getValue()[1],
                        percent(en.getValue()[0], total)))
                .sorted(Comparator.comparingLong(CategorySummary::amount).reversed())
                .toList();

        // 최근 N개월 추이 (오래된 순). 해당 월은 이미 계산한 합계를 그대로 쓴다
        List<MonthTotal> trend = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 1; i--) {
            YearMonth m = month.minusMonths(i);
            trend.add(new MonthTotal(m, i == 1 ? previousTotal : sumOf(m)));
        }
        trend.add(new MonthTotal(month, total));

        Optional<ExpenseBudget> budget = budgetRepository.findFirstByMonthLessThanEqualOrderByMonthDesc(month.toString());
        Long budgetAmount = budget.map(ExpenseBudget::getAmount).orElse(null);

        return new ExpenseSummaryResponse(
                month,
                total,
                expenses.size(),
                previousTotal,
                previousTotal == 0 ? null : percent(total - previousTotal, previousTotal),
                budgetAmount,
                budget.map(b -> YearMonth.parse(b.getMonth())).orElse(null),
                budgetAmount == null || budgetAmount == 0 ? null : percent(total, budgetAmount),
                byCategory,
                trend);
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest r) {
        Expense expense = Expense.builder()
                .spentOn(r.spentOn())
                .amount(r.amount())
                .category(r.category())
                .paymentMethod(r.paymentMethod())
                .merchant(blankToNull(r.merchant()))
                .memo(blankToNull(r.memo()))
                .build();
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest r) {
        Expense expense = findById(id);
        expense.update(r.spentOn(), r.amount(), r.category(), r.paymentMethod(),
                blankToNull(r.merchant()), blankToNull(r.memo()));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(findById(id));
    }

    /** 해당 월 예산 설정. 0 이면 해당 월 예산을 지워 이전 달 예산을 이어받게 한다 */
    @Transactional
    public void setBudget(BudgetRequest r) {
        String key = r.month().toString();
        if (r.amount() == 0) {
            budgetRepository.deleteById(key);
            return;
        }
        budgetRepository.findById(key).ifPresentOrElse(
                b -> b.changeAmount(r.amount()),
                () -> budgetRepository.save(new ExpenseBudget(key, r.amount())));
    }

    private long sumOf(YearMonth month) {
        return expenseRepository.sumAmount(start(month), end(month));
    }

    private Expense findById(Long id) {
        return expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
    }

    private static LocalDate start(YearMonth month) {
        return month.atDay(1);
    }

    /** 다음 달 1일 (배타적 끝) */
    private static LocalDate end(YearMonth month) {
        return month.plusMonths(1).atDay(1);
    }

    /** part / whole × 100, 소수 둘째 자리. whole 0 이면 0 */
    private static BigDecimal percent(long part, long whole) {
        if (whole == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
