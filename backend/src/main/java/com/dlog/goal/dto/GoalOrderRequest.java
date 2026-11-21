package com.dlog.goal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 그 해의 목표 id 전체를 보여줄 순서대로 */
public record GoalOrderRequest(@NotNull Integer year, @NotNull List<@NotNull Long> ids) {
}
