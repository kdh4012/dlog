package com.dlog.asset;

import com.dlog.asset.dto.AssetRequest;
import com.dlog.asset.dto.AssetResponse;
import com.dlog.asset.dto.AssetSummaryResponse;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;

    public List<AssetResponse> getAll() {
        return assetRepository.findAllByOrderByAssetTypeAscNameAsc().stream()
                .map(AssetResponse::from)
                .toList();
    }

    public AssetSummaryResponse getSummary() {
        return AssetSummaryResponse.from(assetRepository.findAll());
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
        Asset asset = findById(id);
        asset.update(r.assetType(), r.name(), r.ticker(), r.account(), r.quantity(), r.avgPrice(),
                r.currentPrice(), r.currency(), r.memo());
        assetRepository.flush(); // updatedAt 반영
        return AssetResponse.from(asset);
    }

    @Transactional
    public void delete(Long id) {
        assetRepository.delete(findById(id));
    }

    private Asset findById(Long id) {
        return assetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }
}
