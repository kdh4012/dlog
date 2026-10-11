package com.dlog.expense;

import com.dlog.expense.dto.FixedExpenseRequest;
import com.dlog.expense.dto.FixedExpenseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 고정지출 템플릿 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/fixed-expenses")
@RequiredArgsConstructor
public class FixedExpenseController {

    private final FixedExpenseService fixedExpenseService;

    @GetMapping
    public List<FixedExpenseResponse> getAll() {
        return fixedExpenseService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FixedExpenseResponse create(@Valid @RequestBody FixedExpenseRequest request) {
        return fixedExpenseService.create(request);
    }

    @PutMapping("/{id}")
    public FixedExpenseResponse update(@PathVariable Long id, @Valid @RequestBody FixedExpenseRequest request) {
        return fixedExpenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        fixedExpenseService.delete(id);
    }
}
