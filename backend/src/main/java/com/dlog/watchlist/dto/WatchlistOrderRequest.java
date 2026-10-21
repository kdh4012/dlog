package com.dlog.watchlist.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 관심 종목 전체 id 를 표시할 순서대로 */
public record WatchlistOrderRequest(@NotNull List<@NotNull Long> ids) {
}
