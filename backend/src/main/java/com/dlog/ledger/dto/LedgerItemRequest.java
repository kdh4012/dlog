package com.dlog.ledger.dto;

import com.dlog.ledger.LedgerKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** kind 는 등록 시에만 사용 (수정 시 무시) */
public record LedgerItemRequest(
        @NotNull LedgerKind kind,
        @NotBlank @Size(max = 100) String name,
        @Positive @Max(1_000_000_000_000_000L) long amount,
        @Size(max = 500) String memo
) {
}
