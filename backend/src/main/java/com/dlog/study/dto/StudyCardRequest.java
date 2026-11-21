package com.dlog.study.dto;

import com.dlog.study.StudyCategory;
import com.dlog.study.StudyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** types 는 기술·업무(KNOWLEDGE) 카드에만 쓰고, 면접 질문이면 무시한다 */
public record StudyCardRequest(
        @NotNull StudyCategory category,
        Set<StudyType> types,
        @NotBlank @Size(max = 500) String question,
        @NotBlank String answer,
        Integer sortOrder
) {
}
