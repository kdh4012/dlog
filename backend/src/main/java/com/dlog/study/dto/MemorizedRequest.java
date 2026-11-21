package com.dlog.study.dto;

import jakarta.validation.constraints.NotNull;

public record MemorizedRequest(@NotNull Boolean memorized) {
}
