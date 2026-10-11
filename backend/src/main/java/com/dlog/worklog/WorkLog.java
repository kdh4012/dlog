package com.dlog.worklog;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 지금까지 수행한 프로젝트/작업 이력 */
@Getter
@Entity
@Table(name = "work_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 회사 또는 소속 */
    @Column(length = 100)
    private String organization;

    /** 담당 역할 */
    @Column(length = 100)
    private String role;

    @Column(nullable = false)
    private LocalDate startDate;

    /** null 이면 진행 중 */
    private LocalDate endDate;

    /** 쉼표 구분 기술 스택 (예: C++,MFC,Spring) */
    @Column(length = 500)
    private String techStack;

    /** 마크다운 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    private WorkLog(String title, String organization, String role, LocalDate startDate, LocalDate endDate,
                    String techStack, String content) {
        this.title = title;
        this.organization = organization;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.techStack = techStack;
        this.content = content;
    }

    public void update(String title, String organization, String role, LocalDate startDate, LocalDate endDate,
                       String techStack, String content) {
        this.title = title;
        this.organization = organization;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.techStack = techStack;
        this.content = content;
    }
}
