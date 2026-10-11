package com.dlog.asset;

import com.dlog.asset.dto.AssetRequest;
import com.dlog.asset.dto.AssetResponse;
import com.dlog.asset.dto.AssetSummaryResponse;
import com.dlog.asset.fx.ExchangeRateService;
import com.dlog.asset.sync.SyncedHolding;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final ExchangeRateService exchangeRateService;

    public List<AssetResponse> getAll() {
        return assetRepository.findAllByOrderByAssetTypeAscNameAsc().stream()
                .map(AssetResponse::from)
                .toList();
    }

    /** 환율 조회(외부 HTTP)가 DB 커넥션을 붙잡지 않도록 트랜잭션 밖에서 실행 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AssetSummaryResponse getSummary() {
        return AssetSummaryResponse.from(assetRepository.findAll(), exchangeRateService.current());
    }

    public AssetResponse get(Long id) {
        return AssetResponse.from(findById(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest r) {
        Asset asset = Asset.builder()
                .assetType(r.assetType())
                .name(r.name())
                .ticker(r.ticker())
                .account(r.account())
                .quantity(r.quantity())
                .avgPrice(r.avgPrice())
                .currentPrice(r.currentPrice())
                .currency(r.currency())
                .memo(r.memo())
                .build();
        return AssetResponse.from(assetRepository.saveAndFlush(asset));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest r) {
        Asset asset = findManualById(id);
        asset.update(r.assetType(), r.name(), r.ticker(), r.account(), r.quantity(), r.avgPrice(),
                r.currentPrice(), r.currency(), r.memo());
        assetRepository.flush(); // updatedAt 반영
        return AssetResponse.from(asset);
    }

    @Transactional
    public void delete(Long id) {
        assetRepository.delete(findManualById(id));
    }

    /**
     * 출처의 보유 자산을 동기화 결과와 일치시킨다. (account, ticker) 기준으로
     * 있으면 갱신, 없으면 추가, 결과에서 사라진 자산(전량 매도 등)은 삭제한다.
     *
     * @return 동기화 후 해당 출처의 자산 수
     */
    @Transactional
    public int replaceSynced(AssetSource source, List<SyncedHolding> holdings) {
        if (!source.isSynced()) {
            throw new IllegalArgumentException("동기화 대상 출처가 아닙니다: " + source);
        }
        Map<String, Asset> existing = assetRepository.findAllBySource(source).stream()
                .collect(Collectors.toMap(Asset::syncKey, Function.identity()));

        for (SyncedHolding holding : holdings) {
            Asset asset = existing.remove(holding.syncKey());
            if (asset == null) {
                assetRepository.save(Asset.synced(source, holding));
            } else {
                asset.applySync(holding);
            }
        }
        assetRepository.deleteAll(existing.values());
        return holdings.size();
    }

    private Asset findById(Long id) {
        return assetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }

    /** 동기화 자산은 다음 동기화 때 덮어써지므로 직접 수정·삭제를 막는다 */
    private Asset findManualById(Long id) {
        Asset asset = findById(id);
        if (asset.getSource().isSynced()) {
            throw new BusinessException(ErrorCode.ASSET_SYNCED_READONLY);
        }
        return asset;
    }
}
