package com.dlog.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;

/** 항목을 채운(이월했거나 직접 입력한) 달. 한 번 채운 달은 항목을 다 지워도 다시 이월하지 않는다 */
@Getter
@Entity
@Table(name = "ledger_month")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerMonth {

    /** yyyy-MM */
    @Id
    @Column(length = 7)
    private String ym;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerMonth(YearMonth month) {
        this.ym = month.toString();
        this.createdAt = LocalDateTime.now();
    }
}
