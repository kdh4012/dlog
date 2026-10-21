package com.dlog.watchlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 미국 주식 시세 조회. 출처를 우선순위대로 시도하고(토스증권 → Yahoo) 종목별로 5분 캐시한다.
 * <ul>
 *   <li>없는 티커는 시세 없음으로 캐시해 반복 호출하지 않는다.</li>
 *   <li>모든 출처가 실패하면 5분 동안 새 조회를 건너뛴다
 *       (관심 종목 화면은 이전 시세 또는 빈 시세로 정상 동작).</li>
 * </ul>
 */
@Slf4j
@Service
public class StockQuoteService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration RETRY_INTERVAL = Duration.ofMinutes(5);

    /** @Order 순으로 주입된다 */
    private final List<QuoteSource> sources;
    private final Map<String, Cached> cache = new HashMap<>();
    private Instant unavailableUntil = Instant.EPOCH;

    private record Cached(StockQuote quote, Instant expiresAt) {
    }

    StockQuoteService(List<QuoteSource> sources) {
        this.sources = sources;
    }

    public synchronized Optional<StockQuote> quote(String ticker) {
        return Optional.ofNullable(quotes(List.of(ticker)).get(ticker));
    }

    /** 티커 → 시세. 시세를 못 구한 티커는 결과에 없다 */
    public synchronized Map<String, StockQuote> quotes(Collection<String> tickers) {
        Instant now = Instant.now();
        List<String> stale = tickers.stream()
                .distinct()
                .filter(t -> !cache.containsKey(t) || now.isAfter(cache.get(t).expiresAt()))
                .toList();
        if (!stale.isEmpty() && !now.isBefore(unavailableUntil)) {
            refresh(stale, now);
        }

        Map<String, StockQuote> result = new HashMap<>();
        for (String ticker : tickers) {
            Cached cached = cache.get(ticker);
            if (cached != null && cached.quote() != null) {
                result.put(ticker, cached.quote()); // 장애 중이면 만료된 이전 값이라도 보여준다
            }
        }
        return result;
    }

    private void refresh(List<String> tickers, Instant now) {
        for (QuoteSource source : sources) {
            if (!source.isAvailable()) {
                continue;
            }
            try {
                Map<String, StockQuote> fetched = source.fetch(tickers);
                // 결과에 없는 티커는 '시세 없음'으로 캐시
                tickers.forEach(t -> cache.put(t, new Cached(fetched.get(t), now.plus(CACHE_TTL))));
                return;
            } catch (RuntimeException e) {
                log.warn("{} 시세 조회 실패, 다음 출처 시도: {}", source.name(), e.getMessage());
            }
        }
        log.warn("모든 시세 출처 실패, {}분 동안 시세 조회 생략", RETRY_INTERVAL.toMinutes());
        unavailableUntil = now.plus(RETRY_INTERVAL);
    }
}
