package com.dlog.expense;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 월 지출 예산 (원). 키는 yyyy-MM 문자열 */
@Getter
@Entity
@Table(name = "expense_budget")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseBudget extends BaseTimeEntity {

    @Id
    @Column(name = "budget_month", length = 7)
    private String month;

    @Column(nullable = false)
    private long amount;

    public ExpenseBudget(String month, long amount) {
        this.month = month;
        this.amount = amount;
    }

    public void changeAmount(long amount) {
        this.amount = amount;
    }
}
