package com.dlog.resolution;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 빈 문자열이면 다짐을 지운다 */
public record ResolutionRequest(@NotNull @Size(max = 1000) String content) {
}
