package com.dlog.goal;

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

/** 한 해의 목표 하나 */
@Getter
@Entity
@Table(name = "goal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 목표 연도 (예: 2026). 등록 후 바꾸지 않는다 */
    @Column(name = "goal_year", nullable = false)
    private int year;

    @Column(nullable = false, length = 200)
    private String title;

    /** 달성했는지 */
    @Column(nullable = false)
    private boolean done;

    /** 같은 해 안에서의 표시 순서 (작을수록 위) */
    @Column(nullable = false)
    private int sortOrder;

    public Goal(int year, String title, int sortOrder) {
        this.year = year;
        this.title = title;
        this.sortOrder = sortOrder;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDone(boolean done) {
        this.done = done;
    }

    public void changeOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
