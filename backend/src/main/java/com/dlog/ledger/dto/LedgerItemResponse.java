package com.dlog.ledger.dto;

import com.dlog.ledger.LedgerItem;
import com.dlog.ledger.LedgerKind;

public record LedgerItemResponse(Long id, LedgerKind kind, String name, long amount, String memo) {

    public static LedgerItemResponse from(LedgerItem i) {
        return new LedgerItemResponse(i.getId(), i.getKind(), i.getName(), i.getAmount(), i.getMemo());
    }
}
