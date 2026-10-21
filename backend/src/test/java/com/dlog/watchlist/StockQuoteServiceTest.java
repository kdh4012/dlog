package com.dlog.watchlist;

import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockQuoteServiceTest {

    private final FakeApiServer server = new FakeApiServer();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static StockQuoteService yahoo(String baseUrl) {
        return new StockQuoteService(List.of(new YahooQuoteSource(baseUrl)));
    }

    /** 고정 결과를 주거나 장애를 내는 출처 */
    private static QuoteSource source(boolean available, Map<String, StockQuote> quotes, RuntimeException error) {
        return new QuoteSource() {
            @Override
            public String name() {
                return "fake";
            }

            @Override
            public boolean isAvailable() {
                return available;
            }

            @Override
            public Map<String, StockQuote> fetch(List<String> tickers) {
                if (error != null) {
                    throw error;
                }
                return quotes;
            }
        };
    }

    /** Yahoo 차트 API 응답 형식 (필요 없는 필드 포함) */
    private static String chart(String symbol, String price, String prevClose) {
        return """
                {"chart":{"result":[{"meta":{"currency":"USD","symbol":"%s","exchangeName":"NMS",
                 "shortName":"%s Inc.","regularMarketPrice":%s,"chartPreviousClose":%s,
                 "regularMarketTime":1760040000,"instrumentType":"EQUITY"},
                 "timestamp":[1760040000],"indicators":{"quote":[{"close":[%s]}]}}],"error":null}}"""
                .formatted(symbol, symbol, price, prevClose, price);
    }

    @Test
    void 현재가와_전일종가를_읽고_5분_캐시() {
        server.route("/v8/finance/chart/AAPL", ex -> Response.ok(chart("AAPL", "227.5", "225")));
        StockQuoteService service = yahoo(server.baseUrl());

        StockQuote q = service.quote("AAPL").orElseThrow();
        service.quote("AAPL");

        assertThat(q.price()).isEqualByComparingTo("227.5");
        assertThat(q.previousClose()).isEqualByComparingTo("225");
        assertThat(q.change()).isEqualByComparingTo("2.5");
        assertThat(q.name()).isEqualTo("AAPL Inc.");
        assertThat(q.currency()).isEqualTo("USD");
        assertThat(q.quotedAt()).isEqualTo(Instant.ofEpochSecond(1760040000));
        assertThat(server.count("GET /v8/finance/chart/AAPL")).isEqualTo(1);
        assertThat(server.requests.get(0)).contains("range=1d").contains("interval=1d");
    }

    @Test
    void 없는_티커는_그_종목만_비우고_나머지는_조회() {
        server.route("/v8/finance/chart/AAPL", ex -> Response.ok(chart("AAPL", "227.5", "225")))
                .route("/v8/finance/chart/NOPE", ex -> new Response(404,
                        "{\"chart\":{\"result\":null,\"error\":{\"code\":\"Not Found\",\"description\":\"No data found\"}}}"));
        StockQuoteService service = yahoo(server.baseUrl());

        Map<String, StockQuote> quotes = service.quotes(List.of("NOPE", "AAPL"));
        service.quotes(List.of("NOPE"));

        assertThat(quotes).containsOnlyKeys("AAPL");
        assertThat(server.count("GET /v8/finance/chart/NOPE")).isEqualTo(1); // 없는 티커도 캐시
    }

    @Test
    void 서버_오류면_잠시_모든_조회를_건너뛴다() {
        server.route("/v8/finance/chart/AAPL", ex -> new Response(503, "{}"))
                .route("/v8/finance/chart/MSFT", ex -> Response.ok(chart("MSFT", "410", "400")));
        StockQuoteService service = yahoo(server.baseUrl());

        Map<String, StockQuote> quotes = service.quotes(List.of("AAPL", "MSFT"));

        assertThat(quotes).isEmpty();
        assertThat(server.count("GET /v8/finance/chart/MSFT")).isZero();
    }

    @Test
    void 연결이_안되면_시세_없이_빈_결과() {
        StockQuoteService service = yahoo("http://127.0.0.1:9");
        assertThat(service.quote("AAPL")).isEmpty();
    }

    @Test
    void 전일종가가_없어도_현재가만으로_동작() {
        server.route("/v8/finance/chart/TSLA", ex -> Response.ok("""
                {"chart":{"result":[{"meta":{"currency":"USD","symbol":"TSLA","regularMarketPrice":250}}],"error":null}}"""));
        StockQuote q = yahoo(server.baseUrl()).quote("TSLA").orElseThrow();

        assertThat(q.price()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(q.change()).isNull();
        assertThat(q.name()).isNull();
    }

    @Test
    void 앞_출처가_장애면_다음_출처로_조회() {
        server.route("/v8/finance/chart/AAPL", ex -> Response.ok(chart("AAPL", "227.5", "225")));
        StockQuoteService service = new StockQuoteService(List.of(
                source(true, Map.of(), new IllegalStateException("토스 장애")),
                new YahooQuoteSource(server.baseUrl())));

        assertThat(service.quote("AAPL")).map(StockQuote::price).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("227.5"));
    }

    @Test
    void 쓸_수_없는_출처는_건너뛰고_앞_출처_결과를_쓴다() {
        StockQuote toss = new StockQuote("AAPL", "애플", new BigDecimal("230"), new BigDecimal("228"), "USD", null);
        StockQuoteService service = new StockQuoteService(List.of(
                source(false, Map.of(), new IllegalStateException("호출되면 안 됨")),
                source(true, Map.of("AAPL", toss), null),
                new YahooQuoteSource(server.baseUrl())));

        assertThat(service.quotes(List.of("AAPL", "NOPE"))).containsExactly(Map.entry("AAPL", toss));
        assertThat(server.requests).isEmpty();
    }
}
