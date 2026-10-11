package com.dlog.expense;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
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

import java.time.LocalDate;
import java.time.YearMonth;

/** 고정지출 템플릿. 매월 1일 날짜로 지출 내역에 반영된다 */
@Getter
@Entity
@Table(name = "fixed_expense")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpense extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    /** yyyy-MM */
    @Column(nullable = false, length = 7)
    private String startMonth;

    /** yyyy-MM, null 이면 계속 */
    @Column(length = 7)
    private String endMonth;

    @Column(length = 500)
    private String memo;

    @Builder
    private FixedExpense(String name, long amount, ExpenseCategory category,
                         YearMonth startMonth, YearMonth endMonth, String memo) {
        update(name, amount, category, startMonth, endMonth, memo);
    }

    public void update(String name, long amount, ExpenseCategory category,
                       YearMonth startMonth, YearMonth endMonth, String memo) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.startMonth = startMonth.toString();
        this.endMonth = endMonth == null ? null : endMonth.toString();
        this.memo = memo;
    }

    public YearMonth startYearMonth() {
        return YearMonth.parse(startMonth);
    }

    public YearMonth endYearMonth() {
        return endMonth == null ? null : YearMonth.parse(endMonth);
    }

    /** 해당 월에 반영 대상인지 */
    public boolean appliesTo(YearMonth month) {
        return !month.isBefore(startYearMonth()) && (endMonth == null || !month.isAfter(endYearMonth()));
    }

    /** 해당 월에 반영될 날짜 (매월 1일) */
    public LocalDate dateIn(YearMonth month) {
        return month.atDay(1);
    }
}
