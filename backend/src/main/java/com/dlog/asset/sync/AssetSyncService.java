package com.dlog.asset.sync;

import com.dlog.asset.AssetService;
import com.dlog.asset.AssetSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 외부 API(업비트·토스증권) 보유 자산 동기화.
 * 출처별로 독립 실행해 한 곳이 실패해도 나머지는 갱신되며, 실패 사유는 상태로 노출한다.
 * 결과 상태는 메모리에만 보관한다 (단일 인스턴스 운영 전제).
 */
@Slf4j
@Service
public class AssetSyncService {

    private final List<HoldingProvider> providers;
    private final AssetService assetService;
    private final Map<AssetSource, SyncStatusResponse> statuses = new EnumMap<>(AssetSource.class);

    public AssetSyncService(List<HoldingProvider> providers, AssetService assetService) {
        this.providers = providers.stream().sorted(Comparator.comparing(HoldingProvider::source)).toList();
        this.assetService = assetService;
    }

    /**
     * 키가 설정된 모든 출처를 동기화한다. 스케줄러와 수동 요청이 겹치면
     * 토스 토큰 재발급이 서로를 무효화하므로 동시에 하나만 실행한다.
     */
    public synchronized List<SyncStatusResponse> syncAll() {
        for (HoldingProvider provider : providers) {
            if (provider.isConfigured()) {
                statuses.put(provider.source(), sync(provider));
            }
        }
        return getStatuses();
    }

    public synchronized List<SyncStatusResponse> getStatuses() {
        return providers.stream()
                .map(p -> statuses.getOrDefault(p.source(), SyncStatusResponse.notYet(p.source(), p.isConfigured())))
                .toList();
    }

    private SyncStatusResponse sync(HoldingProvider provider) {
        AssetSource source = provider.source();
        LocalDateTime now = LocalDateTime.now();
        try {
            int count = assetService.replaceSynced(source, provider.fetchHoldings());
            log.info("{} 자산 동기화 완료: {}건", source, count);
            return new SyncStatusResponse(source, true, now, true, count, null);
        } catch (ExternalApiException e) {
            log.warn("{} 자산 동기화 실패: {}", source, e.getMessage(), e);
            return new SyncStatusResponse(source, true, now, false, null, e.getMessage());
        } catch (RuntimeException e) {
            // 응답 형식 변경(숫자 파싱 실패 등)·DB 오류도 다른 출처 동기화를 막지 않도록 상태로 기록
            log.error("{} 자산 동기화 중 예기치 않은 오류", source, e);
            return new SyncStatusResponse(source, true, now, false, null, "예기치 않은 오류: " + e.getMessage());
        }
    }
}
