package com.dlog.asset.sync;

import com.dlog.asset.AssetSource;

import java.time.LocalDateTime;

/**
 * 출처별 마지막 동기화 결과.
 *
 * @param configured   API 키 설정 여부. false 면 동기화하지 않는다
 * @param lastSyncedAt 마지막 시도 시각 (성공·실패 무관). 서버 재시작 후 첫 동기화 전에는 null
 * @param success      마지막 시도 성공 여부
 * @param assetCount   마지막 성공 시 동기화된 자산 수
 * @param error        마지막 시도 실패 사유
 */
public record SyncStatusResponse(
        AssetSource source,
        boolean configured,
        LocalDateTime lastSyncedAt,
        boolean success,
        Integer assetCount,
        String error
) {
    static SyncStatusResponse notYet(AssetSource source, boolean configured) {
        return new SyncStatusResponse(source, configured, null, false, null, null);
    }
}
