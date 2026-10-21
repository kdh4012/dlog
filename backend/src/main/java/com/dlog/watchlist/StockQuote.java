package com.dlog.watchlist;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 종목 시세.
 *
 * @param price         현재가 (장중이면 실시간에 가까운 값, 장 마감 후면 종가)
 * @param previousClose 전일 종가
 * @param name          거래소 표시 이름 (없을 수 있음)
 * @param quotedAt      시세 기준 시각
 */
public record StockQuote(
        String ticker,
        String name,
        BigDecimal price,
        BigDecimal previousClose,
        String currency,
        Instant quotedAt
) {
    public BigDecimal change() {
        return previousClose == null ? null : price.subtract(previousClose);
    }
}
