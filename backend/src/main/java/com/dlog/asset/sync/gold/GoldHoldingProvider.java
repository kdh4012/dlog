package com.dlog.asset.sync.gold;

import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.HoldingProvider;
import com.dlog.asset.sync.SyncedHolding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 금 현물: 직접 입력한 보유량에 KRX 금 시세를 곱해 자산(source=GOLD)으로 동기화한다.
 * 보유량을 입력해야 동기화 대상이 되고, 시세를 못 받으면 마지막 시세(없으면 매입 단가)로 평가한다.
 */
@Component
@RequiredArgsConstructor
public class GoldHoldingProvider implements HoldingProvider {

    static final String NAME = "금현물 (KRX)";
    static final String TICKER = "KRX-GOLD";
    static final String ACCOUNT = "금 현물";

    private final GoldHoldingRepository repository;
    private final GoldPriceClient priceClient;

    @Override
    public AssetSource source() {
        return AssetSource.GOLD;
    }

    /** 보유량을 입력했는지 (시세 키가 없어도 매입 단가로 평가할 수 있다) */
    @Override
    public boolean isConfigured() {
        return repository.existsById(GoldHolding.SINGLE_ID);
    }

    @Override
    @Transactional
    public List<SyncedHolding> fetchHoldings() {
        Optional<GoldHolding> found = repository.findById(GoldHolding.SINGLE_ID);
        if (found.isEmpty()) {
            return List.of();
        }
        GoldHolding holding = found.get();
        // 시세 조회 실패(ExternalApiException)는 동기화 실패로 상태에 남긴다. 직전 자산 값은 그대로 유지된다
        priceClient.latest().ifPresent(p -> holding.updatePrice(p.pricePerGram(), p.date()));
        return List.of(new SyncedHolding(AssetType.ETC, NAME, TICKER, ACCOUNT,
                holding.getGrams(), holding.getAvgPrice(), holding.currentPrice(), Currency.KRW));
    }
}
