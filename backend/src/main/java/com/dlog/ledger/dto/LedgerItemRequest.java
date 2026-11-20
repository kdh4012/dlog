package com.dlog.ledger.dto;

import com.dlog.ledger.LedgerKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * kind·month 는 등록 시에만 사용 (수정 시 무시).
 *
 * @param month yyyy-MM. 수입·고정지출은 필수, 부채는 무시
 */
public record LedgerItemRequest(
        @NotNull LedgerKind kind,
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "yyyy-MM 형식이어야 합니다.") String month,
        @NotBlank @Size(max = 100) String name,
        @Positive @Max(1_000_000_000_000_000L) long amount,
        @Size(max = 500) String memo
) {
}
