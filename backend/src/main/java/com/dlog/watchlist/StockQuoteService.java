package com.dlog.watchlist;

import com.dlog.asset.sync.ExternalApiClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 미국 주식 시세 조회. Yahoo Finance 차트 API(키 불필요)의 meta 값을 쓰고 종목별로 5분 캐시한다.
 * <ul>
 *   <li>없는 티커(404 등 4xx)는 시세 없음으로 캐시해 반복 호출하지 않는다.</li>
 *   <li>네트워크 오류·5xx 면 외부 API 가 막힌 것으로 보고 5분 동안 모든 조회를 건너뛴다
 *       (관심 종목 화면은 시세만 비운 채로 정상 동작).</li>
 * </ul>
 */
@Slf4j
@Service
public class StockQuoteService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration RETRY_INTERVAL = Duration.ofMinutes(5);

    private final RestClient client;
    private final Map<String, Cached> cache = new HashMap<>();
    private Instant unavailableUntil = Instant.EPOCH;

    private record Cached(StockQuote quote, Instant expiresAt) {
    }

    public StockQuoteService(@Value("${dlog.quote.base-url:https://query1.finance.yahoo.com}") String baseUrl) {
        this.client = ExternalApiClients.create(baseUrl);
    }

    public synchronized Optional<StockQuote> quote(String ticker) {
        return Optional.ofNullable(quotes(List.of(ticker)).get(ticker));
    }

    /** 티커 → 시세. 시세를 못 구한 티커는 결과에 없다 */
    public synchronized Map<String, StockQuote> quotes(Collection<String> tickers) {
        Instant now = Instant.now();
        Map<String, StockQuote> result = new HashMap<>();
        for (String ticker : tickers) {
            Cached cached = cache.get(ticker);
            if (cached == null || now.isAfter(cached.expiresAt())) {
                if (now.isBefore(unavailableUntil)) {
                    continue; // 외부 API 장애 중: 새로 조회하지 않음
                }
                cached = fetchAndCache(ticker, now);
            }
            if (cached != null && cached.quote() != null) {
                result.put(ticker, cached.quote());
            }
        }
        return result;
    }

    private Cached fetchAndCache(String ticker, Instant now) {
        try {
            Cached fresh = new Cached(fetch(ticker), now.plus(CACHE_TTL));
            cache.put(ticker, fresh);
            return fresh;
        } catch (HttpClientErrorException e) {
            // 없는 티커 등: 이 종목만 시세 없음으로 캐시
            log.info("시세 없음 {}: HTTP {}", ticker, e.getStatusCode().value());
            Cached empty = new Cached(null, now.plus(CACHE_TTL));
            cache.put(ticker, empty);
            return empty;
        } catch (RestClientException | IllegalStateException e) {
            log.warn("시세 조회 실패 {}, {}분 동안 시세 생략: {}", ticker, RETRY_INTERVAL.toMinutes(), e.getMessage());
            unavailableUntil = now.plus(RETRY_INTERVAL);
            return cache.get(ticker); // 이전 값이 있으면(만료됐어도) 그대로 보여준다
        }
    }

    /** GET /v8/finance/chart/{ticker}?range=1d&interval=1d */
    private StockQuote fetch(String ticker) {
        ChartResponse body = client.get()
                .uri(b -> b.path("/v8/finance/chart/{ticker}").queryParam("range", "1d").queryParam("interval", "1d")
                        .build(ticker))
                // 기본 Java User-Agent 는 차단되는 경우가 있어 브라우저 형식으로 보낸다
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; DLog)")
                .retrieve()
                .body(ChartResponse.class);
        if (body == null || body.chart() == null || body.chart().result() == null || body.chart().result().isEmpty()) {
            throw new IllegalStateException("시세 응답이 비어 있습니다.");
        }
        Meta meta = body.chart().result().get(0).meta();
        if (meta == null || meta.regularMarketPrice() == null) {
            throw new IllegalStateException("시세 응답에 현재가가 없습니다.");
        }
        BigDecimal previousClose = meta.previousClose() != null ? meta.previousClose() : meta.chartPreviousClose();
        String name = meta.shortName() != null ? meta.shortName() : meta.longName();
        Instant quotedAt = meta.regularMarketTime() == null ? null : Instant.ofEpochSecond(meta.regularMarketTime());
        return new StockQuote(ticker, name, meta.regularMarketPrice(), previousClose, meta.currency(), quotedAt);
    }

    record ChartResponse(Chart chart) {
    }

    record Chart(List<Result> result) {
    }

    record Result(Meta meta) {
    }

    record Meta(String currency, String symbol, String shortName, String longName, BigDecimal regularMarketPrice,
                BigDecimal previousClose, BigDecimal chartPreviousClose, Long regularMarketTime) {
    }
}
