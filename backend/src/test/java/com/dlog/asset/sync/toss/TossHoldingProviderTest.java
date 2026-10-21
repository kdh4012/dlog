package com.dlog.asset.sync.toss;

import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import com.dlog.asset.sync.SyncedHolding;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TossHoldingProviderTest {

    private static final String HOLDINGS = """
            {"result":{
              "totalPurchaseAmount":{"krw":"1000000","usd":"500.00"},
              "marketValue":{"amount":{"krw":"1100000"},"amountAfterCost":{"krw":"1090000"}},
              "profitLoss":{"amount":{"krw":"100000"},"amountAfterCost":{"krw":"90000"},"rate":"0.1","rateAfterCost":"0.09"},
              "dailyProfitLoss":{"amount":{"krw":"1000"},"rate":"0.001"},
              "items":[
                {"symbol":"005930","name":"삼성전자","marketCountry":"KR","currency":"KRW","quantity":"10",
                 "lastPrice":"72000","averagePurchasePrice":"65000.5",
                 "marketValue":{"purchaseAmount":"650005","amount":"720000","amountAfterCost":"718000"},
                 "profitLoss":{"amount":"69995","amountAfterCost":"67995","rate":"0.1077","rateAfterCost":"0.1046"},
                 "dailyProfitLoss":{"amount":"1000","rate":"0.0014"},"cost":{"commission":"108","tax":"1440"}},
                {"symbol":"AAPL","name":"애플","marketCountry":"US","currency":"USD","quantity":"1.5",
                 "lastPrice":"230.12","averagePurchasePrice":"200.00",
                 "marketValue":{"purchaseAmount":"300","amount":"345.18","amountAfterCost":"344.8"},
                 "profitLoss":{"amount":"45.18","amountAfterCost":"44.8","rate":"0.1506","rateAfterCost":"0.1493"},
                 "dailyProfitLoss":{"amount":"1","rate":"0.003"},"cost":{"commission":"0.35"}},
                {"symbol":"0700","name":"텐센트","marketCountry":"HK","currency":"HKD","quantity":"1",
                 "lastPrice":"400","averagePurchasePrice":"380",
                 "marketValue":{"purchaseAmount":"380","amount":"400","amountAfterCost":"399"},
                 "profitLoss":{"amount":"20","amountAfterCost":"19","rate":"0.05","rateAfterCost":"0.05"},
                 "dailyProfitLoss":{"amount":"0","rate":"0"},"cost":{"commission":"1"}}
              ]}}""";

    private final FakeApiServer server = new FakeApiServer();
    private final AtomicInteger issued = new AtomicInteger();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private TossHoldingProvider provider() {
        return new TossHoldingProvider(new TossApiClient(new TossProperties(server.baseUrl(), "tsck_live_id", "s_secret")));
    }

    /** 토큰 발급: 호출될 때마다 token-1, token-2 ... 를 발급 (재발급 시 이전 토큰 무효) */
    private FakeApiServer withTokenEndpoint() {
        return server.route("/oauth2/token", ex -> {
            String form = body(ex);
            assertThat(form).contains("grant_type=client_credentials", "client_id=tsck_live_id", "client_secret=s_secret");
            return Response.ok("""
                    {"access_token":"token-%d","token_type":"Bearer","expires_in":86400}""".formatted(issued.incrementAndGet()));
        });
    }

    private boolean isCurrentToken(HttpExchange ex) {
        return ("Bearer token-" + issued.get()).equals(ex.getRequestHeaders().getFirst("Authorization"));
    }

    @Test
    void 계좌별_보유주식을_변환하고_토큰은_재사용() {
        withTokenEndpoint()
                .route("/api/v1/accounts", ex -> isCurrentToken(ex)
                        ? Response.ok("""
                        {"result":[{"accountNo":"12345678901","accountSeq":7,"accountType":"BROKERAGE"}]}""")
                        : new Response(401, "{\"error\":{\"code\":\"unauthorized\"}}"))
                .route("/api/v1/holdings", ex -> {
                    assertThat(ex.getRequestHeaders().getFirst("X-Tossinvest-Account")).isEqualTo("7");
                    return isCurrentToken(ex) ? Response.ok(HOLDINGS)
                            : new Response(401, "{\"error\":{\"code\":\"unauthorized\"}}");
                });
        TossHoldingProvider provider = provider();

        List<SyncedHolding> holdings = provider.fetchHoldings();
        provider.fetchHoldings();

        assertThat(holdings).containsExactly(
                new SyncedHolding(AssetType.STOCK, "삼성전자", "005930", "토스증권 ··8901",
                        new BigDecimal("10"), new BigDecimal("65000.5"), new BigDecimal("72000"), Currency.KRW),
                new SyncedHolding(AssetType.STOCK, "애플", "AAPL", "토스증권 ··8901",
                        new BigDecimal("1.5"), new BigDecimal("200.00"), new BigDecimal("230.12"), Currency.USD));
        // 두 번 동기화해도 토큰은 한 번만 발급 (재발급하면 이전 토큰이 무효화되므로)
        assertThat(issued.get()).isEqualTo(1);
    }

    @Test
    void 토큰이_무효화되면_한번만_재발급해_재시도() {
        AtomicInteger holdingsCalls = new AtomicInteger();
        withTokenEndpoint()
                .route("/api/v1/accounts", ex -> Response.ok("""
                        {"result":[{"accountNo":"1","accountSeq":7,"accountType":"BROKERAGE"}]}"""))
                .route("/api/v1/holdings", ex -> holdingsCalls.incrementAndGet() == 1
                        // 다른 곳에서 재발급해 기존 토큰이 무효화된 상황
                        ? new Response(401, "{\"error\":{\"code\":\"unauthorized\"}}")
                        : Response.ok("{\"result\":{\"items\":[]}}"));

        assertThat(provider().fetchHoldings()).isEmpty();
        assertThat(issued.get()).isEqualTo(2);
        assertThat(holdingsCalls.get()).isEqualTo(2);
    }

    @Test
    void 허용IP_미등록은_원인을_포함한_예외() {
        server.route("/oauth2/token", ex -> new Response(403, """
                {"error":"access_denied","error_description":"허용되지 않은 IP 주소입니다."}"""));

        assertThatThrownBy(() -> provider().fetchHoldings())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("토스증권 토큰 발급")
                .hasMessageContaining("HTTP 403")
                .hasMessageContaining("허용되지 않은 IP 주소입니다.");
    }

    private static String body(HttpExchange ex) {
        try {
            return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
