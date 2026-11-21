package com.dlog.study.dto;

import com.dlog.study.StudyCard;
import com.dlog.study.StudyCategory;
import com.dlog.study.StudyType;

import java.util.List;

/** @param types 유형 (엔티티가 EnumSet 이라 선언 순서) */
public record StudyCardResponse(
        Long id,
        StudyCategory category,
        List<StudyType> types,
        String question,
        String answer,
        int sortOrder,
        boolean memorized
) {
    public static StudyCardResponse from(StudyCard c) {
        return new StudyCardResponse(c.getId(), c.getCategory(), List.copyOf(c.getTypes()), c.getQuestion(), c.getAnswer(),
                c.getSortOrder(), c.isMemorized());
    }
}
