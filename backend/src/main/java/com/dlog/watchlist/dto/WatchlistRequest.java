package com.dlog.watchlist.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** ticker 는 등록 시에만 사용 (수정 시 무시) */
public record WatchlistRequest(
        @NotBlank @Pattern(regexp = "^\\s*[A-Za-z][A-Za-z0-9.\\-]{0,9}\\s*$", message = "티커 형식이 올바르지 않습니다. (예: AAPL, BRK-B)")
        String ticker,
        @Size(max = 100) String name,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 4) BigDecimal targetPrice,
        @Size(max = 500) String memo
) {
}
