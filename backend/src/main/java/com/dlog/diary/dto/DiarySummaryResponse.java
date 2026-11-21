package com.dlog.diary.dto;

import com.dlog.diary.DiaryEntry;

import java.time.LocalDate;

/**
 * 목록용 일기 요약.
 *
 * @param preview 본문 앞부분 (마크다운 기호·줄바꿈 정리, 최대 120자)
 */
public record DiarySummaryResponse(Long id, LocalDate entryDate, String title, String preview) {

    private static final int PREVIEW_LENGTH = 120;

    public static DiarySummaryResponse from(DiaryEntry e) {
        return new DiarySummaryResponse(e.getId(), e.getEntryDate(), e.getTitle(), preview(e.getContent()));
    }

    static String preview(String content) {
        // 제목(#)·목록(-, *, 1.)·인용(>) 기호와 강조(**, _, `)를 걷어내고 공백을 하나로 합친다
        String plain = content
                .replaceAll("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+\\.)\\s+", "")
                .replaceAll("[*_`~]", "")
                .replaceAll("\\s+", " ")
                .strip();
        return plain.length() <= PREVIEW_LENGTH ? plain : plain.substring(0, PREVIEW_LENGTH) + "…";
    }
}
