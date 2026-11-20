package com.dlog.ledger;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

/** 가계부 항목. 수입·고정지출은 그 달(ledgerMonth)의 금액, 부채는 월 구분 없는 현재 잔액 (원 단위) */
@Getter
@Entity
@Table(name = "ledger_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerKind kind;

    /** yyyy-MM. 부채는 null */
    @Column(length = 7)
    private String ledgerMonth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long amount;

    @Column(length = 500)
    private String memo;

    public LedgerItem(LedgerKind kind, YearMonth month, String name, long amount, String memo) {
        this.kind = kind;
        this.ledgerMonth = month == null ? null : month.toString();
        update(name, amount, memo);
    }

    /** 다음 달로 이월할 때 같은 내용으로 복사 */
    public LedgerItem copyTo(YearMonth month) {
        return new LedgerItem(kind, month, name, amount, memo);
    }

    /** 구분(kind)은 바꾸지 않는다 (다른 구분이면 삭제 후 다시 등록) */
    public void update(String name, long amount, String memo) {
        this.name = name;
        this.amount = amount;
        this.memo = memo;
    }
}
