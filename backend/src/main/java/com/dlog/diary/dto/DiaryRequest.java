package com.dlog.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** title 은 선택 */
public record DiaryRequest(
        @NotNull LocalDate entryDate,
        @Size(max = 200) String title,
        @NotBlank @Size(max = 100_000) String content
) {
}
