package com.dlog.asset.history;

import com.dlog.asset.AssetService;
import com.dlog.asset.Currency;
import com.dlog.asset.dto.AssetSummaryResponse;
import com.dlog.asset.dto.AssetSummaryResponse.CurrencySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** 일별 총 자산 기록. 동기화가 끝날 때마다 오늘(한국 시간) 값을 갱신한다 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetHistoryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AssetService assetService;
    private final AssetSnapshotRepository repository;

    @Transactional(readOnly = true)
    public List<AssetHistoryResponse> getAll() {
        return repository.findAllByOrderBySnapDateAsc().stream().map(AssetHistoryResponse::from).toList();
    }

    /**
     * 오늘의 총 자산을 기록한다. 자산이 없거나, 환율이 없어 달러 자산을 원화로 합칠 수 없으면 건너뛴다
     * (잘못된 값이 추이에 섞이지 않도록).
     */
    @Transactional
    public void recordToday() {
        AssetSummaryResponse summary = assetService.getSummary();
        Optional<Totals> totals = totalsKrw(summary);
        if (totals.isEmpty()) {
            log.debug("총 자산 기록 건너뜀 (자산 없음 또는 환율 없음)");
            return;
        }
        Totals t = totals.get();
        LocalDate today = LocalDate.now(KST);
        repository.findById(today).ifPresentOrElse(
                s -> s.update(t.eval(), t.cost()),
                () -> repository.save(new AssetSnapshot(today, t.eval(), t.cost())));
    }

    private record Totals(BigDecimal eval, BigDecimal cost) {
    }

    /** 평가·매입 원화 합계. 환율이 없어도 원화 자산뿐이면 그대로 합친다 */
    private static Optional<Totals> totalsKrw(AssetSummaryResponse summary) {
        if (summary.currencies().isEmpty()) {
            return Optional.empty();
        }
        if (summary.total() != null) {
            return Optional.of(new Totals(summary.total().evalKrw(), summary.total().costKrw()));
        }
        List<CurrencySummary> currencies = summary.currencies();
        if (currencies.size() == 1 && currencies.get(0).currency() == Currency.KRW) {
            return Optional.of(new Totals(currencies.get(0).evalAmount(), currencies.get(0).costAmount()));
        }
        return Optional.empty();
    }
}
