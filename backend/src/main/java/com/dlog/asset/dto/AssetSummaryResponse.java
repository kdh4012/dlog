package com.dlog.asset.dto;

import com.dlog.asset.Asset;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.fx.ExchangeRates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 통화별 자산 요약과, 환율이 있으면 전체 자산의 원화·달러 환산 합계.
 *
 * @param total        전체 합계. 환율을 못 가져왔거나 환율 없는 통화 자산이 있으면 null
 * @param exchangeRate 환산에 쓴 환율. 환율을 못 가져왔으면 null
 */
public record AssetSummaryResponse(
        List<CurrencySummary> currencies,
        TotalSummary total,
        ExchangeRateInfo exchangeRate
) {

    public record CurrencySummary(
            Currency currency,
            BigDecimal costAmount,
            BigDecimal evalAmount,
            BigDecimal profitAmount,
            BigDecimal profitRate,
            List<TypeSummary> byType
    ) {
    }

    /** weight: 해당 통화 평가금액 중 비중(%) */
    public record TypeSummary(AssetType assetType, BigDecimal evalAmount, BigDecimal weight) {
    }

    /**
     * 전체 자산을 원화로 합산한 뒤 달러로 환산한 값. 수익률은 원화 기준.
     *
     * @param byType 자산 유형별 원화 환산 평가금액과 전체 중 비중(%)
     */
    public record TotalSummary(
            BigDecimal costKrw,
            BigDecimal evalKrw,
            BigDecimal profitKrw,
            BigDecimal costUsd,
            BigDecimal evalUsd,
            BigDecimal profitUsd,
            BigDecimal profitRate,
            List<TypeSummary> byType
    ) {
    }

    /** krwPerUnit: 1 단위당 원화 (화면에서 종목별 환산에 사용) */
    public record ExchangeRateInfo(LocalDate date, String source, Map<Currency, BigDecimal> krwPerUnit) {
    }

    public static AssetSummaryResponse from(List<Asset> assets, Optional<ExchangeRates> rates) {
        // 통화 → 자산 목록
        Map<Currency, List<Asset>> byCurrency = assets.stream()
                .collect(Collectors.groupingBy(Asset::getCurrency, () -> new EnumMap<>(Currency.class),
                        Collectors.toList()));

        List<CurrencySummary> currencies = byCurrency.entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .toList();

        TotalSummary total = rates
                .filter(r -> r.krwPerUnit().keySet().containsAll(byCurrency.keySet()))
                .map(r -> total(assets, currencies, r))
                .orElse(null);
        ExchangeRateInfo info = rates
                .map(r -> new ExchangeRateInfo(r.date(), ExchangeRates.SOURCE, r.krwPerUnit()))
                .orElse(null);
        return new AssetSummaryResponse(currencies, total, info);
    }

    private static TotalSummary total(List<Asset> assets, List<CurrencySummary> currencies, ExchangeRates rates) {
        BigDecimal cost = sum(currencies.stream().map(c -> rates.toKrw(c.costAmount(), c.currency())).toList());
        BigDecimal eval = sum(currencies.stream().map(c -> rates.toKrw(c.evalAmount(), c.currency())).toList());
        BigDecimal profit = eval.subtract(cost);
        return new TotalSummary(cost, eval, profit,
                rates.toUsd(cost), rates.toUsd(eval), rates.toUsd(profit), ProfitRate.of(cost, eval),
                weights(assets, a -> rates.toKrw(a.evalAmount(), a.getCurrency()), eval));
    }

    /** 자산 유형별 평가금액 합계와 total 대비 비중. 큰 순서 */
    private static List<TypeSummary> weights(List<Asset> assets, Function<Asset, BigDecimal> evalOf, BigDecimal total) {
        Map<AssetType, BigDecimal> evalByType = assets.stream()
                .collect(Collectors.groupingBy(Asset::getAssetType, () -> new EnumMap<>(AssetType.class),
                        Collectors.reducing(BigDecimal.ZERO, evalOf, BigDecimal::add)));
        return evalByType.entrySet().stream()
                .map(t -> new TypeSummary(t.getKey(), t.getValue(), ProfitRate.weight(t.getValue(), total)))
                .sorted(Comparator.comparing(TypeSummary::evalAmount).reversed())
                .toList();
    }

    private static CurrencySummary summarize(Currency currency, List<Asset> assets) {
        BigDecimal cost = sum(assets.stream().map(Asset::costAmount).toList());
        BigDecimal eval = sum(assets.stream().map(Asset::evalAmount).toList());
        return new CurrencySummary(currency, cost, eval, eval.subtract(cost), ProfitRate.of(cost, eval),
                weights(assets, Asset::evalAmount, eval));
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
