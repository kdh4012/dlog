package com.dlog.watchlist;

import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import com.dlog.asset.sync.toss.TossApiClient;
import com.dlog.asset.sync.toss.TossProperties;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TossQuoteSourceTest {

    private static final String NOT_FOUND = """
            {"error":{"requestId":"r","code":"not-found","message":"종목을 찾을 수 없습니다."}}""";

    private final FakeApiServer server = new FakeApiServer();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private TossQuoteSource source() {
        server.route("/oauth2/token", ex -> Response.ok("""
                {"access_token":"token-1","token_type":"Bearer","expires_in":86400}"""));
        return new TossQuoteSource(new TossApiClient(new TossProperties(server.baseUrl(), "id", "secret")));
    }

    private static String query(HttpExchange ex) {
        return ex.getRequestURI().getQuery();
    }

    /** 정규장 중(22:30 KST 개장 후 1시간) AAPL 시세 + 일봉 2개 */
    private FakeApiServer withAapl() {
        return server
                .route("/api/v1/prices", ex -> Response.ok("""
                        {"result":[{"symbol":"AAPL","timestamp":"2026-10-20T23:30:00.456+09:00","lastPrice":"230.50","currency":"USD"}]}"""))
                .route("/api/v1/candles", ex -> Response.ok("""
                        {"result":{"candles":[
                          {"timestamp":"2026-10-20T22:30:00+09:00","openPrice":"229","highPrice":"231","lowPrice":"228","closePrice":"230.50","volume":"100","currency":"USD"},
                          {"timestamp":"2026-10-17T22:30:00+09:00","openPrice":"226","highPrice":"229","lowPrice":"225","closePrice":"228.00","volume":"100","currency":"USD"}
                        ],"nextBefore":"2026-10-17T22:30:00+09:00"}}"""))
                .route("/api/v1/stocks", ex -> Response.ok("""
                        {"result":[{"symbol":"AAPL","name":"애플","englishName":"APPLE INC","market":"NASDAQ","currency":"USD","status":"ACTIVE"}]}"""));
    }

    @Test
    void 현재가_한글명_전일종가를_합친다() {
        TossQuoteSource source = source();
        withAapl();

        StockQuote q = source.fetch(List.of("AAPL")).get("AAPL");
        source.fetch(List.of("AAPL"));

        assertThat(q.price()).isEqualByComparingTo("230.50");
        assertThat(q.previousClose()).isEqualByComparingTo("228.00");
        assertThat(q.change()).isEqualByComparingTo("2.50");
        assertThat(q.name()).isEqualTo("애플");
        assertThat(q.currency()).isEqualTo("USD");
        assertThat(q.quotedAt()).isEqualTo(OffsetDateTime.parse("2026-10-20T23:30:00.456+09:00").toInstant());
        assertThat(server.requests).anyMatch(r -> r.startsWith("GET /api/v1/candles") && r.contains("symbol=AAPL")
                && r.contains("interval=1d") && r.contains("count=2"));
        // 종목명은 처음 한 번만 조회
        assertThat(server.count("GET /api/v1/stocks")).isEqualTo(1);
        assertThat(server.count("POST /oauth2/token")).isEqualTo(1);
    }

    @Test
    void 없는_종목이_섞이면_한_종목씩_다시_조회해_그_종목만_뺀다() {
        TossQuoteSource source = source();
        withAapl().route("/api/v1/prices", ex -> switch (query(ex)) {
            case "symbols=AAPL" -> Response.ok("""
                    {"result":[{"symbol":"AAPL","timestamp":null,"lastPrice":"230.50","currency":"USD"}]}""");
            default -> new Response(404, NOT_FOUND);
        });

        Map<String, StockQuote> quotes = source.fetch(List.of("AAPL", "ZZZZ"));

        assertThat(quotes).containsOnlyKeys("AAPL");
        assertThat(server.requests).contains("GET /api/v1/prices?symbols=AAPL,ZZZZ",
                "GET /api/v1/prices?symbols=AAPL", "GET /api/v1/prices?symbols=ZZZZ");
    }

    @Test
    void 일봉_조회가_실패해도_현재가는_준다() {
        TossQuoteSource source = source();
        withAapl().route("/api/v1/candles", ex -> new Response(429, "{\"error\":{\"code\":\"too-many-requests\"}}"));

        StockQuote q = source.fetch(List.of("AAPL")).get("AAPL");

        assertThat(q.price()).isEqualByComparingTo("230.50");
        assertThat(q.previousClose()).isNull();
    }

    @Test
    void 서버_오류는_예외로_다음_출처에_넘긴다() {
        TossQuoteSource source = source();
        server.route("/api/v1/prices", ex -> new Response(500, "{}"));

        assertThatThrownBy(() -> source.fetch(List.of("AAPL"))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void 키가_없으면_쓰지_않는다() {
        TossQuoteSource source = new TossQuoteSource(new TossApiClient(new TossProperties(server.baseUrl(), "", "")));
        assertThat(source.isAvailable()).isFalse();
    }

    private static TossQuoteSource.Candle candle(String time, String close) {
        return new TossQuoteSource.Candle(OffsetDateTime.parse(time), new BigDecimal(close));
    }

    @Test
    void 정규장_이후_체결이면_최신_봉_종가가_기준() {
        List<TossQuoteSource.Candle> candles = List.of(
                candle("2026-10-20T22:30:00+09:00", "230"),   // 10/20 정규장 (KST 22:30 ~ 05:00)
                candle("2026-10-17T22:30:00+09:00", "228"));

        // 정규장 중·마감 직후 체결 → 이전 봉 종가
        assertThat(TossQuoteSource.previousClose(candles, OffsetDateTime.parse("2026-10-21T04:59:00+09:00").toInstant()))
                .isEqualByComparingTo("228");
        // 다음 날 데이마켓(KST 10:00~) 체결 → 최신 봉 종가
        assertThat(TossQuoteSource.previousClose(candles, OffsetDateTime.parse("2026-10-21T11:00:00+09:00").toInstant()))
                .isEqualByComparingTo("230");
        assertThat(TossQuoteSource.previousClose(List.of(), null)).isNull();
    }
}
