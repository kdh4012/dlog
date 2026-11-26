package com.dlog.asset.sync.gold;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param grams         보유량 (g). 입력 전이면 0
 * @param currentPrice  평가에 쓰는 시세 (원/g). 시세를 못 받았으면 매입 단가
 * @param priceDate     시세 기준일. 시세를 받은 적 없으면 null
 * @param priceApiReady 시세 API 키가 설정되어 있는지
 */
public record GoldHoldingResponse(BigDecimal grams, BigDecimal avgPrice, BigDecimal currentPrice, LocalDate priceDate,
                                  boolean priceApiReady) {

    static GoldHoldingResponse of(GoldHolding h, boolean priceApiReady) {
        return new GoldHoldingResponse(h.getGrams(), h.getAvgPrice(), h.currentPrice(), h.getPriceDate(), priceApiReady);
    }

    static GoldHoldingResponse empty(boolean priceApiReady) {
        return new GoldHoldingResponse(BigDecimal.ZERO, BigDecimal.ZERO, null, null, priceApiReady);
    }
}
