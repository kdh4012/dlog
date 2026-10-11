package com.dlog.worklog.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WorkLogRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 100) String organization,
        @Size(max = 100) String role,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = 500) String techStack,
        String content
) {
    @AssertTrue(message = "종료일은 시작일 이후여야 합니다.")
    public boolean isValidPeriod() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
