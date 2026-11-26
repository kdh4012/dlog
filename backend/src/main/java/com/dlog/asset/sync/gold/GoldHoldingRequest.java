package com.dlog.asset.sync.gold;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 보유량(g)·평균 매입 단가(원/g). 보유량 0 이면 금 자산을 지운다 */
public record GoldHoldingRequest(
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal grams,
        @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal avgPrice
) {
}
