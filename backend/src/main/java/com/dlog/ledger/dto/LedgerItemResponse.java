package com.dlog.ledger.dto;

import com.dlog.ledger.LedgerItem;
import com.dlog.ledger.LedgerKind;

/** @param month yyyy-MM. 부채는 null */
public record LedgerItemResponse(Long id, LedgerKind kind, String month, String name, long amount, String memo) {

    public static LedgerItemResponse from(LedgerItem i) {
        return new LedgerItemResponse(i.getId(), i.getKind(), i.getLedgerMonth(), i.getName(), i.getAmount(), i.getMemo());
    }
}
