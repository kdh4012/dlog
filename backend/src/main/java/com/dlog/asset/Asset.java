package com.dlog.asset;

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

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 보유 자산 (종목/계좌 단위) */
@Getter
@Entity
@Table(name = "asset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 100)
    private String name;

    /** 종목 코드 / 티커 (선택) */
    @Column(length = 30)
    private String ticker;

    /** 보유 계좌 / 증권사 (선택) */
    @Column(length = 100)
    private String account;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    /** 평균 매입 단가 */
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal avgPrice;

    /** 현재가 (수동 입력) */
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(length = 500)
    private String memo;

    @Builder
    private Asset(AssetType assetType, String name, String ticker, String account, BigDecimal quantity,
                  BigDecimal avgPrice, BigDecimal currentPrice, Currency currency, String memo) {
        this.assetType = assetType;
        this.name = name;
        this.ticker = ticker;
        this.account = account;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.memo = memo;
    }

    public void update(AssetType assetType, String name, String ticker, String account, BigDecimal quantity,
                       BigDecimal avgPrice, BigDecimal currentPrice, Currency currency, String memo) {
        this.assetType = assetType;
        this.name = name;
        this.ticker = ticker;
        this.account = account;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.memo = memo;
    }

    /** 매입 금액 = 수량 × 평균단가 */
    public BigDecimal costAmount() {
        return quantity.multiply(avgPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /** 평가 금액 = 수량 × 현재가 */
    public BigDecimal evalAmount() {
        return quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
