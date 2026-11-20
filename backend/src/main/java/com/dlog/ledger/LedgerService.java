package com.dlog.ledger;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.ledger.dto.LedgerItemRequest;
import com.dlog.ledger.dto.LedgerItemResponse;
import com.dlog.ledger.dto.LedgerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

    private final LedgerRepository repository;

    public LedgerResponse get() {
        // 정렬된 목록을 구분별로 나눠도 순서(금액 큰 순)는 유지된다
        Map<LedgerKind, List<LedgerItemResponse>> byKind = repository.findAllByOrderByAmountDescIdAsc().stream()
                .map(LedgerItemResponse::from)
                .collect(Collectors.groupingBy(LedgerItemResponse::kind));
        List<LedgerItemResponse> incomes = byKind.getOrDefault(LedgerKind.INCOME, List.of());
        List<LedgerItemResponse> fixed = byKind.getOrDefault(LedgerKind.FIXED_EXPENSE, List.of());
        List<LedgerItemResponse> debts = byKind.getOrDefault(LedgerKind.DEBT, List.of());

        long incomeTotal = sum(incomes);
        long fixedTotal = sum(fixed);
        return new LedgerResponse(incomeTotal, fixedTotal, incomeTotal - fixedTotal, sum(debts), incomes, fixed, debts);
    }

    @Transactional
    public LedgerItemResponse create(LedgerItemRequest r) {
        return LedgerItemResponse.from(repository.save(new LedgerItem(r.kind(), r.name().trim(), r.amount(), blankToNull(r.memo()))));
    }

    @Transactional
    public LedgerItemResponse update(Long id, LedgerItemRequest r) {
        LedgerItem item = findById(id);
        item.update(r.name().trim(), r.amount(), blankToNull(r.memo()));
        return LedgerItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private LedgerItem findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ITEM_NOT_FOUND));
    }

    private static long sum(List<LedgerItemResponse> items) {
        return items.stream().mapToLong(LedgerItemResponse::amount).sum();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
