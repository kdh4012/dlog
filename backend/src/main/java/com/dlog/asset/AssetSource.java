package com.dlog.asset;

/** 자산 출처. MANUAL 외에는 외부 API 동기화로만 갱신된다 */
public enum AssetSource {
    MANUAL,
    UPBIT,
    TOSS;

    public boolean isSynced() {
        return this != MANUAL;
    }
}
