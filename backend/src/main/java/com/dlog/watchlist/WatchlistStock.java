package com.dlog.watchlist;

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

import java.math.BigDecimal;

/** 관심 종목 (미국 주식, USD) */
@Getter
@Entity
@Table(name = "watchlist_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistStock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대문자 티커 (예: AAPL, BRK-B) */
    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(length = 100)
    private String name;

    /** 목표가 (USD, 선택) */
    @Column(precision = 20, scale = 4)
    private BigDecimal targetPrice;

    @Column(length = 500)
    private String memo;

    public WatchlistStock(String ticker, String name, BigDecimal targetPrice, String memo) {
        this.ticker = ticker;
        update(name, targetPrice, memo);
    }

    /** 티커는 바꾸지 않는다 (다른 종목이면 삭제 후 다시 추가) */
    public void update(String name, BigDecimal targetPrice, String memo) {
        this.name = name;
        this.targetPrice = targetPrice;
        this.memo = memo;
    }
}
