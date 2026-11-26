package com.dlog.asset.sync.gold;

import java.math.BigDecimal;
import java.time.LocalDate;

/** KRX 금 1g 종가와 기준일 */
public record GoldPrice(BigDecimal pricePerGram, LocalDate date) {
}
