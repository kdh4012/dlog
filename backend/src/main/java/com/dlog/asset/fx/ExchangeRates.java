package com.dlog.asset.fx;

import com.dlog.asset.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * 통화별 원화 환산 비율.
 *
 * @param date       환율 기준일 (ECB 고시일, 영업일만 갱신)
 * @param krwPerUnit 1 단위당 원화 (KRW 는 1)
 */
public record ExchangeRates(LocalDate date, Map<Currency, BigDecimal> krwPerUnit) {

    public static final String SOURCE = "ECB (Frankfurter)";

    public BigDecimal toKrw(BigDecimal amount, Currency from) {
        return amount.multiply(rate(from)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal toUsd(BigDecimal krwAmount) {
        return krwAmount.divide(rate(Currency.USD), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(Currency currency) {
        BigDecimal rate = krwPerUnit.get(currency);
        if (rate == null) {
            throw new IllegalStateException("환율 없음: " + currency);
        }
        return rate;
    }
}
