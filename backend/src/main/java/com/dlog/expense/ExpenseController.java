package com.dlog.expense;

import com.dlog.expense.dto.BudgetRequest;
import com.dlog.expense.dto.ExpenseRequest;
import com.dlog.expense.dto.ExpenseResponse;
import com.dlog.expense.dto.ExpenseSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/** 소비 지출 관리 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final FixedExpenseService fixedExpenseService;

    @GetMapping
    public List<ExpenseResponse> getMonth(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth m = orThisMonth(month);
        // 조회 전에 그 달의 고정지출을 지출 내역으로 반영
        fixedExpenseService.applyRange(m, m);
        return expenseService.getMonth(m);
    }

    @GetMapping("/summary")
    public ExpenseSummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth m = orThisMonth(month);
        // 요약의 월별 추이(최근 6개월)에도 고정지출이 들어가도록 함께 반영
        fixedExpenseService.applyRange(m.minusMonths(ExpenseService.TREND_MONTHS - 1), m);
        return expenseService.getSummary(m);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PutMapping("/budget")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBudget(@Valid @RequestBody BudgetRequest request) {
        expenseService.setBudget(request);
    }

    private static YearMonth orThisMonth(YearMonth month) {
        return month != null ? month : FixedExpenseService.thisMonth();
    }
}
