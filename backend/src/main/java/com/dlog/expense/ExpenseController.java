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
import java.time.ZoneId;
import java.util.List;

/** 소비 지출 관리 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    /** month 미지정 시 기준이 되는 '이번 달'. 서버 시간대(UTC)와 무관하게 한국 기준 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ExpenseService expenseService;

    @GetMapping
    public List<ExpenseResponse> getMonth(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return expenseService.getMonth(orThisMonth(month));
    }

    @GetMapping("/summary")
    public ExpenseSummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return expenseService.getSummary(orThisMonth(month));
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
        return month != null ? month : YearMonth.now(KST);
    }
}
