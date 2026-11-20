package com.dlog.ledger;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.ledger.dto.LedgerItemRequest;
import com.dlog.ledger.dto.LedgerItemResponse;
import com.dlog.ledger.dto.LedgerResponse;
import com.dlog.ledger.dto.LedgerResponse.MonthTotal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TREND_MONTHS = 12;

    private final LedgerRepository repository;
    private final LedgerMonthRepository monthRepository;

    /** 이번 달 (한국 시간) */
    public static YearMonth thisMonth() {
        return YearMonth.now(KST);
    }

    /**
     * 아직 채우지 않은 달이면 가장 최근에 채운 달의 수입·고정지출을 복사한다.
     * 사이에 빈 달이 있으면 그 달들도 같은 내용으로 채운다. 미래 달은 채우지 않는다.
     */
    @Transactional
    public void carryOver(YearMonth month) {
        String ym = month.toString();
        if (month.isAfter(thisMonth()) || monthRepository.existsById(ym)) {
            return;
        }
        Optional<LedgerMonth> previous = monthRepository.findTopByYmLessThanOrderByYmDesc(ym);
        if (previous.isEmpty()) {
            return; // 처음 기록하는 달: 복사할 내용이 없으면 직접 입력할 때 채운 달로 표시한다
        }
        List<LedgerItem> source = repository.findAllByLedgerMonthOrderByAmountDescIdAsc(previous.get().getYm());
        // previous 가 month 이전의 가장 최근 달이므로 그 사이 달은 모두 비어 있다
        for (YearMonth m = YearMonth.parse(previous.get().getYm()).plusMonths(1); !m.isAfter(month); m = m.plusMonths(1)) {
            YearMonth target = m;
            repository.saveAll(source.stream().map(i -> i.copyTo(target)).toList());
            monthRepository.save(new LedgerMonth(m));
        }
    }

    public LedgerResponse get(YearMonth month) {
        List<LedgerItemResponse> items = repository.findAllByLedgerMonthOrderByAmountDescIdAsc(month.toString()).stream()
                .map(LedgerItemResponse::from)
                .toList();
        List<LedgerItemResponse> incomes = items.stream().filter(i -> i.kind() == LedgerKind.INCOME).toList();
        List<LedgerItemResponse> fixed = items.stream().filter(i -> i.kind() == LedgerKind.FIXED_EXPENSE).toList();
        List<LedgerItemResponse> debts = repository.findAllByKindOrderByAmountDescIdAsc(LedgerKind.DEBT).stream()
                .map(LedgerItemResponse::from)
                .toList();

        long incomeTotal = sum(incomes);
        long fixedTotal = sum(fixed);
        return new LedgerResponse(month.toString(), incomeTotal, fixedTotal, incomeTotal - fixedTotal, sum(debts),
                incomes, fixed, debts, trend(month));
    }

    /** month 까지 최근 12개월 월별 합계 (기록 없는 달은 0) */
    private List<MonthTotal> trend(YearMonth month) {
        YearMonth from = month.minusMonths(TREND_MONTHS - 1L);
        Map<String, long[]> sums = new HashMap<>(); // ym → [수입, 고정지출]
        for (Object[] row : repository.sumByMonth(from.toString(), month.toString())) {
            long[] s = sums.computeIfAbsent((String) row[0], k -> new long[2]);
            s[row[1] == LedgerKind.INCOME ? 0 : 1] += ((Number) row[2]).longValue();
        }
        List<MonthTotal> trend = new ArrayList<>(TREND_MONTHS);
        for (YearMonth m = from; !m.isAfter(month); m = m.plusMonths(1)) {
            long[] s = sums.getOrDefault(m.toString(), new long[2]);
            trend.add(new MonthTotal(m.toString(), s[0], s[1]));
        }
        return trend;
    }

    @Transactional
    public LedgerItemResponse create(LedgerItemRequest r) {
        YearMonth month = null;
        if (r.kind() != LedgerKind.DEBT) {
            if (r.month() == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            month = YearMonth.parse(r.month());
            // 빈 달에 처음 입력하면 이전 달 내용을 먼저 이월한 뒤 추가하고, 채운 달로 표시한다
            carryOver(month);
            if (!monthRepository.existsById(month.toString())) {
                monthRepository.save(new LedgerMonth(month));
            }
        }
        LedgerItem saved = repository.save(new LedgerItem(r.kind(), month, r.name().trim(), r.amount(), blankToNull(r.memo())));
        return LedgerItemResponse.from(saved);
    }

    /** 그 달 항목만 바뀐다. 이후 새로 채우는 달은 가장 최근 달 내용을 이월한다 */
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
