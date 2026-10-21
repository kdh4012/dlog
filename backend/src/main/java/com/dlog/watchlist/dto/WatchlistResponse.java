package com.dlog.watchlist.dto;

import com.dlog.watchlist.StockQuote;
import com.dlog.watchlist.WatchlistStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 관심 종목 + 시세. 시세를 못 구하면 price 이하가 null.
 *
 * @param changeRate    전일 대비 등락률(%)
 * @param targetGapRate 목표가까지 남은 비율(%) = (목표가 - 현재가) / 현재가 × 100. 음수면 목표가 도달
 */
public record WatchlistResponse(
        Long id,
        String ticker,
        String name,
        BigDecimal targetPrice,
        String memo,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        String currency,
        Instant quotedAt,
        BigDecimal targetGapRate
) {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static WatchlistResponse of(WatchlistStock s, StockQuote q) {
        BigDecimal target = s.getTargetPrice() == null ? null : s.getTargetPrice().stripTrailingZeros();
        if (q == null) {
            return new WatchlistResponse(s.getId(), s.getTicker(), s.getName(), target, s.getMemo(),
                    null, null, null, null, null, null);
        }
        BigDecimal change = q.change();
        BigDecimal changeRate = change == null || q.previousClose().signum() == 0 ? null
                : change.multiply(HUNDRED).divide(q.previousClose(), 2, RoundingMode.HALF_UP);
        BigDecimal targetGap = target == null || q.price().signum() == 0 ? null
                : target.subtract(q.price()).multiply(HUNDRED).divide(q.price(), 2, RoundingMode.HALF_UP);
        return new WatchlistResponse(s.getId(), s.getTicker(), s.getName(), target, s.getMemo(),
                q.price(), change, changeRate, q.currency(), q.quotedAt(), targetGap);
    }
}
