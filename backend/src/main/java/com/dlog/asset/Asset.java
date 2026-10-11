package com.dlog.asset;

import com.dlog.asset.sync.SyncedHolding;
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

    /** quantity / avg_price / current_price 컬럼의 소수 자릿수 */
    private static final int SCALE = 8;

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

    /** 현재가 (수동 입력 또는 동기화 시점 시세) */
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetSource source;

    @Builder
    private Asset(AssetType assetType, String name, String ticker, String account, BigDecimal quantity,
                  BigDecimal avgPrice, BigDecimal currentPrice, Currency currency, String memo, AssetSource source) {
        this.assetType = assetType;
        this.name = name;
        this.ticker = ticker;
        this.account = account;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.currency = currency;
        this.memo = memo;
        this.source = source == null ? AssetSource.MANUAL : source;
    }

    /** 외부 API 동기화 자산 생성 */
    public static Asset synced(AssetSource source, SyncedHolding holding) {
        Asset asset = Asset.builder().source(source).build();
        asset.applySync(holding);
        return asset;
    }

    /** 동기화 결과로 덮어쓴다. DB 컬럼 scale 에 맞춰 반올림 */
    public void applySync(SyncedHolding h) {
        this.assetType = h.assetType();
        this.name = h.name();
        this.ticker = h.ticker();
        this.account = h.account();
        this.quantity = h.quantity().setScale(SCALE, RoundingMode.HALF_UP);
        this.avgPrice = h.avgPrice().setScale(SCALE, RoundingMode.HALF_UP);
        this.currentPrice = h.currentPrice().setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = h.currency();
    }

    /** 동기화 키. {@link SyncedHolding#syncKey()} 와 같은 규칙 */
    public String syncKey() {
        return account + "|" + ticker;
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
