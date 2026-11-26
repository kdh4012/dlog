package com.dlog.asset.history;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 그날 총 자산 (원화) */
public record AssetHistoryResponse(LocalDate date, BigDecimal evalKrw, BigDecimal costKrw) {

    static AssetHistoryResponse from(AssetSnapshot s) {
        return new AssetHistoryResponse(s.getSnapDate(), s.getEvalKrw(), s.getCostKrw());
    }
}
