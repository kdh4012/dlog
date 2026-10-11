package com.dlog.asset.sync.upbit;

import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import com.dlog.asset.sync.SyncedHolding;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpbitHoldingProviderTest {

    private static final String ACCESS_KEY = "test-access-key";
    private static final String SECRET_KEY = "test-secret-key-0123456789abcdef0123456789";

    private final FakeApiServer server = new FakeApiServer();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private UpbitHoldingProvider provider() {
        return new UpbitHoldingProvider(new UpbitProperties(server.baseUrl(), ACCESS_KEY, SECRET_KEY));
    }

    @Test
    void 원화는_현금_코인은_KRW_시세로_변환() {
        AtomicReference<String> authorization = new AtomicReference<>();
        server.route("/v1/accounts", ex -> {
            authorization.set(ex.getRequestHeaders().getFirst("Authorization"));
            // 업비트 응답: 금액·수량은 문자열
            return Response.ok("""
                    [
                      {"currency":"KRW","balance":"150000.5","locked":"0","avg_buy_price":"0","avg_buy_price_modified":false,"unit_currency":"KRW"},
                      {"currency":"BTC","balance":"0.01","locked":"0.005","avg_buy_price":"90000000","avg_buy_price_modified":false,"unit_currency":"KRW"},
                      {"currency":"OLD","balance":"3","locked":"0","avg_buy_price":"10","avg_buy_price_modified":false,"unit_currency":"KRW"},
                      {"currency":"DUST","balance":"0","locked":"0","avg_buy_price":"5","avg_buy_price_modified":false,"unit_currency":"KRW"}
                    ]""");
        }).route("/v1/market/all", ex -> Response.ok("""
                [
                  {"market":"KRW-BTC","korean_name":"비트코인","english_name":"Bitcoin"},
                  {"market":"BTC-ETH","korean_name":"이더리움","english_name":"Ethereum"}
                ]""")
        ).route("/v1/ticker", ex -> Response.ok("""
                [{"market":"KRW-BTC","trade_price":95000000.0,"change":"RISE"}]"""));

        List<SyncedHolding> holdings = provider().fetchHoldings();

        assertThat(holdings).containsExactly(
                new SyncedHolding(AssetType.CASH, "원화 예수금", "KRW", "업비트",
                        new BigDecimal("150000.5"), BigDecimal.ONE, BigDecimal.ONE, Currency.KRW),
                new SyncedHolding(AssetType.CRYPTO, "비트코인", "BTC", "업비트",
                        new BigDecimal("0.015"), new BigDecimal("90000000"), new BigDecimal("95000000.0"), Currency.KRW));
        // KRW 마켓이 없는 OLD 는 제외하고, 시세는 유효한 마켓만 요청
        assertThat(server.requests).contains("GET /v1/ticker?markets=KRW-BTC");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(authorization.get().substring("Bearer ".length()))
                .getPayload();
        assertThat(claims.get("access_key")).isEqualTo(ACCESS_KEY);
        assertThat(claims.get("nonce")).isNotNull();
    }

    @Test
    void 허용IP_미등록등_오류는_응답본문을_포함한_예외() {
        server.route("/v1/accounts", ex -> new Response(401, """
                {"error":{"name":"no_authorization_ip","message":"This is not a verified IP."}}"""));

        assertThatThrownBy(() -> provider().fetchHoldings())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("This is not a verified IP.");
    }
}
