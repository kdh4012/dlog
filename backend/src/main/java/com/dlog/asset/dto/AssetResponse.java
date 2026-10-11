package com.dlog.asset.dto;

import com.dlog.asset.Asset;
import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        AssetType assetType,
        String name,
        String ticker,
        String account,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal currentPrice,
        Currency currency,
        String memo,
        AssetSource source,
        BigDecimal costAmount,
        BigDecimal evalAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate,
        LocalDateTime updatedAt
) {
    public static AssetResponse from(Asset a) {
        BigDecimal cost = a.costAmount();
        BigDecimal eval = a.evalAmount();
        return new AssetResponse(a.getId(), a.getAssetType(), a.getName(), a.getTicker(), a.getAccount(),
                a.getQuantity().stripTrailingZeros(), a.getAvgPrice().stripTrailingZeros(),
                a.getCurrentPrice().stripTrailingZeros(), a.getCurrency(), a.getMemo(), a.getSource(),
                cost, eval, eval.subtract(cost), ProfitRate.of(cost, eval), a.getUpdatedAt());
    }
}
