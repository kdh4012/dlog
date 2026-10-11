package com.dlog.expense;

import com.dlog.expense.dto.FixedExpenseRequest;
import com.dlog.expense.dto.FixedExpenseResponse;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixedExpenseService {

    /** '이번 달' 기준. 서버 시간대(UTC)와 무관하게 한국 기준 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseRunRepository runRepository;
    private final ExpenseRepository expenseRepository;

    public static YearMonth thisMonth() {
        return YearMonth.now(KST);
    }

    public List<FixedExpenseResponse> getAll() {
        YearMonth now = thisMonth();
        return fixedExpenseRepository.findAllByOrderByIdAsc().stream()
                .map(f -> FixedExpenseResponse.from(f, now))
                .toList();
    }

    /**
     * from ~ to 각 달에 아직 반영되지 않은 고정지출을 지출 내역으로 만든다.
     * 미래 달은 미리 만들지 않으며(이번 달까지만), 반영 기록이 있는 달은 내역을 지웠더라도 다시 만들지 않는다.
     * 단일 서버이므로 동시 조회로 인한 중복 생성은 synchronized 로 막는다.
     */
    @Transactional
    public synchronized void applyRange(YearMonth from, YearMonth to) {
        YearMonth last = to.isAfter(thisMonth()) ? thisMonth() : to;
        if (from.isAfter(last)) {
            return;
        }
        List<FixedExpense> templates = fixedExpenseRepository.findAll();
        for (YearMonth m = from; !m.isAfter(last); m = m.plusMonths(1)) {
            for (FixedExpense f : templates) {
                FixedExpenseRun.Key key = new FixedExpenseRun.Key(f.getId(), m.toString());
                if (!f.appliesTo(m) || runRepository.existsById(key)) {
                    continue;
                }
                expenseRepository.save(Expense.fromFixed(f, m));
                runRepository.save(new FixedExpenseRun(f.getId(), m.toString()));
            }
        }
    }

    @Transactional
    public FixedExpenseResponse create(FixedExpenseRequest r) {
        FixedExpense f = FixedExpense.builder()
                .name(r.name().trim())
                .amount(r.amount())
                .category(r.category())
                .startMonth(r.startMonth())
                .endMonth(r.endMonth())
                .memo(blankToNull(r.memo()))
                .build();
        return FixedExpenseResponse.from(fixedExpenseRepository.save(f), thisMonth());
    }

    /** 템플릿만 바꾼다. 이미 반영된 달의 내역은 그대로 (그 달 내역은 지출 목록에서 개별 수정) */
    @Transactional
    public FixedExpenseResponse update(Long id, FixedExpenseRequest r) {
        FixedExpense f = findById(id);
        f.update(r.name().trim(), r.amount(), r.category(),
                r.startMonth(), r.endMonth(), blankToNull(r.memo()));
        return FixedExpenseResponse.from(f, thisMonth());
    }

    /** 템플릿 삭제. 이미 반영된 지난 내역은 고정지출로 남는다 (원본 id 는 DB 에서 null 처리) */
    @Transactional
    public void delete(Long id) {
        fixedExpenseRepository.delete(findById(id));
    }

    private FixedExpense findById(Long id) {
        return fixedExpenseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_NOT_FOUND));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
