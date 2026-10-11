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

/** 소비 지출 1건 (원화) */
@Getter
@Entity
@Table(name = "expense")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate spentOn;

    /** 금액 (원) */
    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    /** 결제 수단. 고정지출에서 반영된 내역은 비어 있을 수 있다 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /** 사용처 (가게 이름 등) */
    @Column(length = 100)
    private String merchant;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExpenseKind kind;

    /** 고정지출에서 반영된 경우 원본 템플릿 id (템플릿 삭제 시 null) */
    private Long fixedExpenseId;

    @Builder
    private Expense(LocalDate spentOn, long amount, ExpenseCategory category, PaymentMethod paymentMethod,
                    String merchant, String memo, ExpenseKind kind, Long fixedExpenseId) {
        this.spentOn = spentOn;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.merchant = merchant;
        this.memo = memo;
        this.kind = kind == null ? ExpenseKind.VARIABLE : kind;
        this.fixedExpenseId = fixedExpenseId;
    }

    /** 해당 월 고정지출 템플릿으로부터 반영 내역 생성 */
    public static Expense fromFixed(FixedExpense f, java.time.YearMonth month) {
        return new Expense(f.dateIn(month), f.getAmount(), f.getCategory(), null,
                f.getName(), f.getMemo(), ExpenseKind.FIXED, f.getId());
    }

    /** 구분(고정/변동)과 원본 템플릿은 유지하고 내용만 수정 */
    public void update(LocalDate spentOn, long amount, ExpenseCategory category, PaymentMethod paymentMethod,
                       String merchant, String memo) {
        this.spentOn = spentOn;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.merchant = merchant;
        this.memo = memo;
    }
}
