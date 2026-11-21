package com.dlog.study.dto;

import com.dlog.study.StudyCard;
import com.dlog.study.StudyCategory;

public record StudyCardResponse(
        Long id,
        StudyCategory category,
        String question,
        String answer,
        int sortOrder,
        boolean memorized
) {
    public static StudyCardResponse from(StudyCard c) {
        return new StudyCardResponse(c.getId(), c.getCategory(), c.getQuestion(), c.getAnswer(),
                c.getSortOrder(), c.isMemorized());
    }
}
