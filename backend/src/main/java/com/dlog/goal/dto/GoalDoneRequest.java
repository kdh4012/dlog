package com.dlog.goal.dto;

import jakarta.validation.constraints.NotNull;

/** 달성 표시 켜기/끄기 */
public record GoalDoneRequest(@NotNull Boolean done) {
}
