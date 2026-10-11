package com.dlog.asset.dto;

import com.dlog.asset.Asset;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 통화별 자산 요약. 환율 변환 없이 통화 단위로 합산한다.
 */
public record AssetSummaryResponse(List<CurrencySummary> currencies) {

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

    public static AssetSummaryResponse from(List<Asset> assets) {
        // 통화 → 자산 목록
        Map<Currency, List<Asset>> byCurrency = assets.stream()
                .collect(Collectors.groupingBy(Asset::getCurrency, () -> new EnumMap<>(Currency.class),
                        Collectors.toList()));

        List<CurrencySummary> result = byCurrency.entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .toList();
        return new AssetSummaryResponse(result);
    }

    private static CurrencySummary summarize(Currency currency, List<Asset> assets) {
        BigDecimal cost = sum(assets.stream().map(Asset::costAmount).toList());
        BigDecimal eval = sum(assets.stream().map(Asset::evalAmount).toList());

        // 자산 유형별 평가금액 합계 및 비중
        Map<AssetType, BigDecimal> evalByType = assets.stream()
                .collect(Collectors.groupingBy(Asset::getAssetType, () -> new EnumMap<>(AssetType.class),
                        Collectors.reducing(BigDecimal.ZERO, Asset::evalAmount, BigDecimal::add)));

        List<TypeSummary> byType = evalByType.entrySet().stream()
                .map(t -> new TypeSummary(t.getKey(), t.getValue(), ProfitRate.weight(t.getValue(), eval)))
                .sorted(Comparator.comparing(TypeSummary::evalAmount).reversed())
                .toList();

        return new CurrencySummary(currency, cost, eval, eval.subtract(cost), ProfitRate.of(cost, eval), byType);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
