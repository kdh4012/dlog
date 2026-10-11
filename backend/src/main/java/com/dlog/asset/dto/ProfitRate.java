package com.dlog.asset.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ProfitRate {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ProfitRate() {
    }

    /** 수익률(%) = (평가 - 매입) / 매입 × 100, 소수 둘째 자리. 매입금액 0 이면 0 */
    static BigDecimal of(BigDecimal cost, BigDecimal eval) {
        if (cost.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return eval.subtract(cost)
                .multiply(HUNDRED)
                .divide(cost, 2, RoundingMode.HALF_UP);
    }

    /** 비중(%) = part / total × 100. total 0 이면 0 */
    static BigDecimal weight(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }
}
