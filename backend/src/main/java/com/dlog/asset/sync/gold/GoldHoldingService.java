package com.dlog.asset.sync.gold;

import com.dlog.asset.AssetService;
import com.dlog.asset.AssetSource;
import com.dlog.asset.sync.AssetSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoldHoldingService {

    private final GoldHoldingRepository repository;
    private final GoldPriceClient priceClient;
    private final AssetSyncService syncService;
    private final AssetService assetService;

    @Transactional(readOnly = true)
    public GoldHoldingResponse get() {
        return repository.findById(GoldHolding.SINGLE_ID)
                .map(h -> GoldHoldingResponse.of(h, priceClient.isConfigured()))
                .orElseGet(() -> GoldHoldingResponse.empty(priceClient.isConfigured()));
    }

    /**
     * 보유량 저장 후 바로 금 자산을 다시 계산한다. 보유량 0 이면 금 자산을 지운다.
     * (저장을 먼저 커밋해야 동기화가 새 값을 읽으므로 이 메서드는 트랜잭션으로 묶지 않는다)
     */
    public GoldHoldingResponse save(GoldHoldingRequest r) {
        if (r.grams().signum() == 0) {
            repository.deleteById(GoldHolding.SINGLE_ID);
            assetService.replaceSynced(AssetSource.GOLD, List.of());
            return GoldHoldingResponse.empty(priceClient.isConfigured());
        }
        GoldHolding holding = repository.findById(GoldHolding.SINGLE_ID)
                .map(h -> {
                    h.change(r.grams(), r.avgPrice());
                    return h;
                })
                .orElseGet(() -> new GoldHolding(r.grams(), r.avgPrice()));
        repository.save(holding);
        syncService.sync(AssetSource.GOLD);
        return get();
    }
}
