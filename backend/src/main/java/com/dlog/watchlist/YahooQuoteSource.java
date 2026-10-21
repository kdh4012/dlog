package com.dlog.watchlist;

import com.dlog.asset.sync.ExternalApiClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Yahoo Finance 차트 API(키 불필요). 토스증권 키가 없거나 토스 조회가 실패했을 때 쓴다 */
@Slf4j
@Order(2)
@Component
class YahooQuoteSource implements QuoteSource {

    private final RestClient client;

    YahooQuoteSource(@Value("${dlog.quote.yahoo-base-url:https://query1.finance.yahoo.com}") String baseUrl) {
        this.client = ExternalApiClients.create(baseUrl);
    }

    @Override
    public String name() {
        return "Yahoo";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<String, StockQuote> fetch(List<String> tickers) {
        Map<String, StockQuote> result = new HashMap<>();
        for (String ticker : tickers) {
            try {
                result.put(ticker, fetch(ticker));
            } catch (HttpClientErrorException e) {
                // 없는 티커 등: 이 종목만 시세 없음
                log.info("Yahoo 시세 없음 {}: HTTP {}", ticker, e.getStatusCode().value());
            }
        }
        return result;
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
