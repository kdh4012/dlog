package com.dlog.diary.dto;

import com.dlog.diary.DiaryEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryResponse(Long id, LocalDate entryDate, String title, String content,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static DiaryResponse from(DiaryEntry e) {
        return new DiaryResponse(e.getId(), e.getEntryDate(), e.getTitle(), e.getContent(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
