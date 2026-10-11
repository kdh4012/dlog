package com.dlog.worklog.dto;

import com.dlog.worklog.WorkLog;

import java.time.LocalDate;

public record WorkLogResponse(
        Long id,
        String title,
        String organization,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        String techStack,
        String content
) {
    public static WorkLogResponse from(WorkLog w) {
        return new WorkLogResponse(w.getId(), w.getTitle(), w.getOrganization(), w.getRole(),
                w.getStartDate(), w.getEndDate(), w.getTechStack(), w.getContent());
    }
}
