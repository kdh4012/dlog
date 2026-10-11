package com.dlog.asset.sync;

import com.dlog.asset.AssetSource;

import java.util.List;

/** 외부 거래소·증권사의 보유 자산 조회 (읽기 전용) */
public interface HoldingProvider {

    AssetSource source();

    /** API 키가 설정되어 있는지. 미설정이면 동기화 대상에서 제외 */
    boolean isConfigured();

    /** @throws ExternalApiException 외부 API 호출 실패 */
    List<SyncedHolding> fetchHoldings();
}
