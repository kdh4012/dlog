package com.dlog.diary;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 일기 한 편. 본문은 마크다운 */
@Getter
@Entity
@Table(name = "diary_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate entryDate;

    /** 선택 */
    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public DiaryEntry(LocalDate entryDate, String title, String content) {
        update(entryDate, title, content);
    }

    public void update(LocalDate entryDate, String title, String content) {
        this.entryDate = entryDate;
        this.title = title;
        this.content = content;
    }
}
