package com.dlog.asset.sync.gold;

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

/** 금 현물 보유량. 하나만 두므로 id 는 항상 1 */
@Getter
@Entity
@Table(name = "gold_holding")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldHolding {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id;

    /** 보유량 (g) */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal grams;

    /** 평균 매입 단가 (원/g) */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal avgPrice;

    /** 마지막으로 받은 시세 (원/g). API 를 못 쓰면 이 값으로 평가한다 */
    @Column(precision = 18, scale = 2)
    private BigDecimal lastPrice;

    private LocalDate priceDate;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public GoldHolding(BigDecimal grams, BigDecimal avgPrice) {
        this.id = SINGLE_ID;
        change(grams, avgPrice);
    }

    public void change(BigDecimal grams, BigDecimal avgPrice) {
        this.grams = grams;
        this.avgPrice = avgPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePrice(BigDecimal price, LocalDate date) {
        this.lastPrice = price;
        this.priceDate = date;
    }

    /** 평가에 쓸 현재가: 받은 시세가 없으면 매입 단가 */
    public BigDecimal currentPrice() {
        return lastPrice != null ? lastPrice : avgPrice;
    }
}
