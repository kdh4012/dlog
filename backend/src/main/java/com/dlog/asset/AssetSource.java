package com.dlog.asset;

/** 자산 출처. MANUAL 외에는 외부 API 동기화로만 갱신된다 */
public enum AssetSource {
    MANUAL,
    UPBIT,
    TOSS,
    /** 한국투자증권 (국내·미국 주식) */
    KIS,
    /** 금 현물: 보유량은 직접 입력, 시세만 공공데이터 API 로 갱신 */
    GOLD;

    public boolean isSynced() {
        return this != MANUAL;
    }
}
