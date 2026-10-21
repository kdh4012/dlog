package com.dlog.watchlist.dto;

import com.dlog.watchlist.StockQuote;
import com.dlog.watchlist.WatchlistStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * 관심 종목 + 시세. 시세를 못 구하면 price 이하가 null.
 *
 * @param changeRate 전일 대비 등락률(%)
 */
public record WatchlistResponse(
        Long id,
        String ticker,
        String name,
        String memo,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        String currency,
        Instant quotedAt
) {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static WatchlistResponse of(WatchlistStock s, StockQuote q) {
        if (q == null) {
            return new WatchlistResponse(s.getId(), s.getTicker(), s.getName(), s.getMemo(),
                    null, null, null, null, null);
        }
        BigDecimal change = q.change();
        BigDecimal changeRate = change == null || q.previousClose().signum() == 0 ? null
                : change.multiply(HUNDRED).divide(q.previousClose(), 2, RoundingMode.HALF_UP);
        return new WatchlistResponse(s.getId(), s.getTicker(), s.getName(), s.getMemo(),
                q.price(), change, changeRate, q.currency(), q.quotedAt());
    }
}
