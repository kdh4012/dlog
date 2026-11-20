package com.dlog.ledger;

import com.dlog.ledger.dto.LedgerItemRequest;
import com.dlog.ledger.dto.LedgerItemResponse;
import com.dlog.ledger.dto.LedgerResponse;
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

/** 가계부(월 수입·월 고정지출·부채) API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * 한 달의 가계부 (month 생략 시 이번 달). 빈 달이면 이전 달 내용을 이월한 뒤 보여준다.
     * 이월은 커밋까지 끝낸 뒤 다음 요청을 받도록 직렬화한다 (동시 조회 시 같은 달 중복 이월 방지)
     */
    @GetMapping
    public synchronized LedgerResponse get(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth m = month != null ? month : LedgerService.thisMonth();
        ledgerService.carryOver(m);
        return ledgerService.get(m);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized LedgerItemResponse create(@Valid @RequestBody LedgerItemRequest request) {
        return ledgerService.create(request);
    }

    @PutMapping("/{id}")
    public LedgerItemResponse update(@PathVariable Long id, @Valid @RequestBody LedgerItemRequest request) {
        return ledgerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ledgerService.delete(id);
    }
}
