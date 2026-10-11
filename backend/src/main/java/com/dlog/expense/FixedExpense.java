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

/** 고정지출 템플릿. 매월 dayOfMonth 에 지출 내역으로 반영된다 */
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

    /** 1~31. 해당 월에 없는 날짜면 말일로 반영 */
    @Column(nullable = false)
    private int dayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** yyyy-MM */
    @Column(nullable = false, length = 7)
    private String startMonth;

    /** yyyy-MM, null 이면 계속 */
    @Column(length = 7)
    private String endMonth;

    @Column(length = 500)
    private String memo;

    @Builder
    private FixedExpense(String name, long amount, int dayOfMonth, ExpenseCategory category,
                         PaymentMethod paymentMethod, YearMonth startMonth, YearMonth endMonth, String memo) {
        update(name, amount, dayOfMonth, category, paymentMethod, startMonth, endMonth, memo);
    }

    public void update(String name, long amount, int dayOfMonth, ExpenseCategory category,
                       PaymentMethod paymentMethod, YearMonth startMonth, YearMonth endMonth, String memo) {
        this.name = name;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.category = category;
        this.paymentMethod = paymentMethod;
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

    /** 해당 월의 결제일 (말일 보정) */
    public LocalDate dateIn(YearMonth month) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }
}
