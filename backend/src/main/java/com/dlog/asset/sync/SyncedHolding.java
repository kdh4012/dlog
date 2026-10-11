package com.dlog.asset.sync;

import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;

import java.math.BigDecimal;

/**
 * 외부 API 에서 가져온 보유 자산 한 건. 같은 출처 안에서 (account, ticker) 가 동기화 키다.
 */
public record SyncedHolding(
        AssetType assetType,
        String name,
        String ticker,
        String account,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal currentPrice,
        Currency currency
) {
    public String syncKey() {
        return account + "|" + ticker;
    }
}
