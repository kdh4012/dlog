package com.dlog.watchlist;

import com.dlog.asset.sync.toss.TossApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 토스증권 Open API 시세 (보유 자산 동기화와 같은 키·토큰 사용).
 * <ul>
 *   <li>현재가: GET /api/v1/prices (다건)</li>
 *   <li>종목명: GET /api/v1/stocks (다건, 한글명. 바뀌지 않으므로 계속 캐시)</li>
 *   <li>전일 종가: GET /api/v1/candles 일봉 2개 (종목별). 실패해도 현재가는 보여준다</li>
 * </ul>
 */
@Slf4j
@Order(1)
@Component
class TossQuoteSource implements QuoteSource {

    /**
     * 미국 정규장 길이(6시간 30분) + 여유. 최신 일봉 시작 시각부터 이 시간 안의 체결이면 그 봉이 진행 중(또는 막 끝난)
     * 정규장이고, 이후의 체결(애프터마켓·데이마켓·프리마켓)이면 최신 봉 종가가 기준가가 된다.
     */
    static final Duration REGULAR_SESSION = Duration.ofHours(7);

    private static final int MAX_SYMBOLS = 200;

    private final TossApiClient api;
    private final Map<String, String> names = new ConcurrentHashMap<>();

    record Price(String symbol, OffsetDateTime timestamp, BigDecimal lastPrice, String currency) {
    }

    record Stock(String symbol, String name) {
    }

    record CandlePage(List<Candle> candles) {
    }

    record Candle(OffsetDateTime timestamp, BigDecimal closePrice) {
    }

    TossQuoteSource(TossApiClient api) {
        this.api = api;
    }

    @Override
    public String name() {
        return "토스증권";
    }

    @Override
    public boolean isAvailable() {
        return api.isConfigured();
    }

    @Override
    public Map<String, StockQuote> fetch(List<String> tickers) {
        Map<String, StockQuote> result = new HashMap<>();
        for (int from = 0; from < tickers.size(); from += MAX_SYMBOLS) {
            List<String> chunk = tickers.subList(from, Math.min(tickers.size(), from + MAX_SYMBOLS));
            for (Price price : prices(chunk)) {
                if (price.lastPrice() == null) {
                    continue;
                }
                result.put(price.symbol(), new StockQuote(price.symbol(), null, price.lastPrice(),
                        previousClose(price), price.currency(),
                        price.timestamp() == null ? null : price.timestamp().toInstant()));
            }
        }
        fillNames(result);
        return result;
    }

    /** 다건 조회. 없는 종목이 섞여 404/400 이 나면 한 종목씩 다시 조회해 없는 종목만 뺀다 */
    private List<Price> prices(List<String> symbols) {
        try {
            return getPrices(symbols);
        } catch (HttpClientErrorException e) {
            if (!isUnknownSymbol(e)) {
                throw e;
            }
            if (symbols.size() == 1) {
                log.info("토스증권 시세 없음 {}: HTTP {}", symbols.get(0), e.getStatusCode().value());
                return List.of();
            }
            return symbols.stream().flatMap(s -> prices(List.of(s)).stream()).toList();
        }
    }

    private List<Price> getPrices(List<String> symbols) {
        return api.get("토스증권 현재가 조회",
                c -> c.get().uri(b -> b.path("/api/v1/prices").queryParam("symbols", String.join(",", symbols)).build()),
                new ParameterizedTypeReference<TossApiClient.Envelope<List<Price>>>() {
                });
    }

    /** 일봉 최신 2개로 전일 종가를 구한다. 조회 실패 시 null (등락만 비움) */
    private BigDecimal previousClose(Price price) {
        try {
            CandlePage page = api.get("토스증권 일봉 조회",
                    c -> c.get().uri(b -> b.path("/api/v1/candles")
                            .queryParam("symbol", price.symbol())
                            .queryParam("interval", "1d")
                            .queryParam("count", 2)
                            .build()),
                    new ParameterizedTypeReference<TossApiClient.Envelope<CandlePage>>() {
                    });
            return previousClose(page.candles(), price.timestamp() == null ? null : price.timestamp().toInstant());
        } catch (RuntimeException e) {
            log.info("토스증권 전일 종가 조회 실패 {}: {}", price.symbol(), e.getMessage());
            return null;
        }
    }

    /**
     * 일봉(최신순)에서 현재가의 비교 기준 종가를 고른다.
     * 현재가 체결 시각이 최신 봉의 정규장 안이면 그 전 봉 종가, 정규장 이후(시간외·데이마켓)면 최신 봉 종가.
     */
    static BigDecimal previousClose(List<Candle> candles, Instant priceTime) {
        if (candles == null || candles.isEmpty()) {
            return null;
        }
        Instant latestOpen = candles.get(0).timestamp().toInstant();
        boolean inLatestSession = priceTime == null || priceTime.isBefore(latestOpen.plus(REGULAR_SESSION));
        if (!inLatestSession) {
            return candles.get(0).closePrice();
        }
        return candles.size() > 1 ? candles.get(1).closePrice() : null;
    }

    /** 처음 보는 종목만 한글 종목명을 조회한다. 실패해도 시세는 그대로 */
    private void fillNames(Map<String, StockQuote> quotes) {
        List<String> missing = quotes.keySet().stream().filter(s -> !names.containsKey(s)).toList();
        if (!missing.isEmpty()) {
            try {
                List<Stock> stocks = api.get("토스증권 종목 정보 조회",
                        c -> c.get().uri(b -> b.path("/api/v1/stocks").queryParam("symbols", String.join(",", missing)).build()),
                        new ParameterizedTypeReference<TossApiClient.Envelope<List<Stock>>>() {
                        });
                stocks.stream().filter(s -> s.name() != null).forEach(s -> names.put(s.symbol(), s.name()));
            } catch (RuntimeException e) {
                log.info("토스증권 종목명 조회 실패: {}", e.getMessage());
            }
        }
        quotes.replaceAll((symbol, q) -> new StockQuote(q.ticker(), names.get(symbol), q.price(), q.previousClose(),
                q.currency(), q.quotedAt()));
    }

    private static boolean isUnknownSymbol(HttpClientErrorException e) {
        return e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST;
    }
}
