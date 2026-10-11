package com.dlog.asset.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 외부 자산 주기 동기화. 간격은 dlog.sync.interval (ISO-8601, 기본 10분) */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AssetSyncScheduler {

    private final AssetSyncService assetSyncService;

    @Scheduled(initialDelayString = "${dlog.sync.initial-delay:PT30S}", fixedDelayString = "${dlog.sync.interval:PT10M}")
    public void sync() {
        assetSyncService.syncAll();
    }
}
