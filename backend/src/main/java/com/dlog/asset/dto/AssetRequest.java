package com.dlog.asset.dto;

import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssetRequest(
        @NotNull AssetType assetType,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String ticker,
        @Size(max = 100) String account,
        @NotNull @PositiveOrZero @Digits(integer = 16, fraction = 8) BigDecimal quantity,
        @NotNull @PositiveOrZero @Digits(integer = 16, fraction = 8) BigDecimal avgPrice,
        @NotNull @PositiveOrZero @Digits(integer = 16, fraction = 8) BigDecimal currentPrice,
        @NotNull Currency currency,
        @Size(max = 500) String memo
) {
}
