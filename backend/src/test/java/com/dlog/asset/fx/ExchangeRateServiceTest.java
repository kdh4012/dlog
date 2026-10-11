package com.dlog.asset.fx;

import com.dlog.asset.Asset;
import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.dto.AssetSummaryResponse;
import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import com.dlog.asset.sync.SyncedHolding;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {

    private final FakeApiServer server = new FakeApiServer();

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void USD_기준_환율을_1단위당_원화로_변환하고_캐시() {
        server.route("/v1/latest", ex -> Response.ok("""
                {"amount":1.0,"base":"USD","date":"2026-10-09","rates":{"EUR":0.8,"JPY":160,"KRW":1400}}"""));
        ExchangeRateService service = new ExchangeRateService(server.baseUrl());

        ExchangeRates rates = service.current().orElseThrow();
        service.current();

        assertThat(rates.date()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(rates.krwPerUnit().get(Currency.KRW)).isEqualByComparingTo("1");
        assertThat(rates.krwPerUnit().get(Currency.USD)).isEqualByComparingTo("1400");
        assertThat(rates.krwPerUnit().get(Currency.JPY)).isEqualByComparingTo("8.75");
        assertThat(rates.krwPerUnit().get(Currency.EUR)).isEqualByComparingTo("1750");
        assertThat(server.count("GET /v1/latest")).isEqualTo(1);
    }

    @Test
    void 조회_실패면_empty() {
        server.route("/v1/latest", ex -> new Response(503, "{}"));

        assertThat(new ExchangeRateService(server.baseUrl()).current()).isEmpty();
    }

    @Test
    void 요약은_전체를_원화로_합산하고_달러로_환산() {
        ExchangeRates rates = new ExchangeRates(LocalDate.of(2026, 10, 9),
                Map.of(Currency.KRW, BigDecimal.ONE, Currency.USD, new BigDecimal("1400")));
        List<Asset> assets = List.of(
                // 매입 100만 → 평가 140만
                asset(AssetType.STOCK, "005930", Currency.KRW, "10", "100000", "140000"),
                // 매입 $100 → 평가 $200 (= 14만 → 28만 원)
                asset(AssetType.ETF, "AAPL", Currency.USD, "1", "100", "200"));

        AssetSummaryResponse summary = AssetSummaryResponse.from(assets, Optional.of(rates));

        AssetSummaryResponse.TotalSummary total = summary.total();
        assertThat(total.costKrw()).isEqualByComparingTo("1140000");
        assertThat(total.evalKrw()).isEqualByComparingTo("1680000");
        assertThat(total.profitKrw()).isEqualByComparingTo("540000");
        assertThat(total.evalUsd()).isEqualByComparingTo("1200");
        assertThat(total.profitRate()).isEqualByComparingTo("47.37");
        // 유형별 비중은 원화 환산 기준: 주식 140만 / ETF 28만 (합 168만)
        assertThat(total.byType()).extracting(AssetSummaryResponse.TypeSummary::assetType)
                .containsExactly(AssetType.STOCK, AssetType.ETF);
        assertThat(total.byType()).extracting(AssetSummaryResponse.TypeSummary::weight)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("83.33"), new BigDecimal("16.67"));
        assertThat(summary.exchangeRate().krwPerUnit()).containsEntry(Currency.USD, new BigDecimal("1400"));
    }

    @Test
    void 환율이_없으면_합계는_null() {
        AssetSummaryResponse summary = AssetSummaryResponse.from(
                List.of(asset(AssetType.STOCK, "AAPL", Currency.USD, "1", "100", "200")), Optional.empty());

        assertThat(summary.total()).isNull();
        assertThat(summary.exchangeRate()).isNull();
        assertThat(summary.currencies()).hasSize(1);
    }

    private static Asset asset(AssetType type, String ticker, Currency currency, String qty, String avg, String price) {
        return Asset.synced(AssetSource.TOSS, new SyncedHolding(type, ticker, ticker, "acc",
                new BigDecimal(qty), new BigDecimal(avg), new BigDecimal(price), currency));
    }
}
