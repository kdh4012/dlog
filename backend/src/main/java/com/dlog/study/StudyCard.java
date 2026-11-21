package com.dlog.study;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

/** 면접 멘트·기술 복습용 질문-답 카드 (관리자 전용) */
@Getter
@Entity
@Table(name = "study_card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyCard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyCategory category;

    /** 기술·업무 카드의 유형 (여러 개). 면접 질문은 비어 있다 */
    @Convert(converter = StudyTypesConverter.class)
    @Column(nullable = false, length = 100)
    private Set<StudyType> types = EnumSet.noneOf(StudyType.class);

    @Column(nullable = false, length = 500)
    private String question;

    /** 마크다운 답변 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    /** 같은 분류 안에서의 표시 순서 (작을수록 위) */
    @Column(nullable = false)
    private int sortOrder;

    @Builder
    private StudyCard(StudyCategory category, Set<StudyType> types, String question, String answer, int sortOrder) {
        this.category = category;
        this.types = typesFor(category, types);
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
    }

    public void update(StudyCategory category, Set<StudyType> types, String question, String answer, int sortOrder) {
        this.category = category;
        this.types = typesFor(category, types);
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
    }

    /** 유형은 기술·업무 카드에만 둔다 */
    private static Set<StudyType> typesFor(StudyCategory category, Set<StudyType> types) {
        if (category != StudyCategory.KNOWLEDGE || types == null || types.isEmpty()) {
            return EnumSet.noneOf(StudyType.class);
        }
        return EnumSet.copyOf(types);
    }
}
