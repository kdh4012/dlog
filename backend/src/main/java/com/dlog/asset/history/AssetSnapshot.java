package com.dlog.asset.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 하루의 총 자산 (원화). 같은 날은 마지막 동기화 값으로 덮어쓴다 */
@Getter
@Entity
@Table(name = "asset_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetSnapshot {

    @Id
    private LocalDate snapDate;

    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal evalKrw;

    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal costKrw;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AssetSnapshot(LocalDate snapDate, BigDecimal evalKrw, BigDecimal costKrw) {
        this.snapDate = snapDate;
        update(evalKrw, costKrw);
    }

    public void update(BigDecimal evalKrw, BigDecimal costKrw) {
        this.evalKrw = evalKrw;
        this.costKrw = costKrw;
        this.updatedAt = LocalDateTime.now();
    }
}
