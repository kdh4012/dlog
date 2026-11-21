package com.dlog.goal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** year 는 등록 시에만 사용 (수정 시 무시, 생략하면 올해) */
public record GoalRequest(
        @Min(2000) @Max(2100) Integer year,
        @NotBlank @Size(max = 200) String title
) {
}
