package com.dlog.asset.sync.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 업비트 응답 DTO. 금액·수량은 문자열 또는 숫자로 오므로 String 으로 받아 BigDecimal 로 변환한다 */
final class UpbitDtos {

    private UpbitDtos() {
    }

    /** GET /v1/accounts */
    record Account(
            String currency,
            String balance,
            String locked,
            @JsonProperty("avg_buy_price") String avgBuyPrice,
            @JsonProperty("unit_currency") String unitCurrency
    ) {
    }

    /** GET /v1/market/all */
    record Market(
            String market,
            @JsonProperty("korean_name") String koreanName
    ) {
    }

    /** GET /v1/ticker */
    record Ticker(
            String market,
            @JsonProperty("trade_price") String tradePrice
    ) {
    }
}
