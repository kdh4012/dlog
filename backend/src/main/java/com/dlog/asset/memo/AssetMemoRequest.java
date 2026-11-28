package com.dlog.asset.memo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 빈 문자열이면 메모를 지운다 */
public record AssetMemoRequest(@NotNull @Size(max = 2000) String content) {
}
