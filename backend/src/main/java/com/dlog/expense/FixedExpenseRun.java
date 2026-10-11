package com.dlog.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 고정지출을 해당 월에 반영했다는 기록 (반영 지출을 지워도 재생성 방지) */
@Getter
@Entity
@Table(name = "fixed_expense_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpenseRun {

    @EmbeddedId
    private Key id;

    public FixedExpenseRun(Long fixedExpenseId, String month) {
        this.id = new Key(fixedExpenseId, month);
    }

    @Getter
    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "fixed_expense_id")
        private Long fixedExpenseId;

        @Column(name = "run_month", length = 7)
        private String month;

        public Key(Long fixedExpenseId, String month) {
            this.fixedExpenseId = fixedExpenseId;
            this.month = month;
        }
    }
}
