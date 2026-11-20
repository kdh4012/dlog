package com.dlog.ledger;

import com.dlog.ledger.dto.LedgerItemRequest;
import com.dlog.ledger.dto.LedgerItemResponse;
import com.dlog.ledger.dto.LedgerResponse;
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

/** 가계부(월 수입·월 고정지출·부채) API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping
    public LedgerResponse get() {
        return ledgerService.get();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerItemResponse create(@Valid @RequestBody LedgerItemRequest request) {
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
