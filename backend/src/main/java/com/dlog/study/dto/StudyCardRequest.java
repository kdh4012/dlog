package com.dlog.study.dto;

import com.dlog.study.StudyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudyCardRequest(
        @NotNull StudyCategory category,
        @NotBlank @Size(max = 500) String question,
        @NotBlank String answer,
        Integer sortOrder
) {
}
