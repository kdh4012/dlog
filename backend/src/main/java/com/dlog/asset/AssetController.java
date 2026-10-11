package com.dlog.asset;

import com.dlog.asset.dto.AssetRequest;
import com.dlog.asset.dto.AssetResponse;
import com.dlog.asset.dto.AssetSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 자산 포트폴리오 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public List<AssetResponse> getAll() {
        return assetService.getAll();
    }

    @GetMapping("/summary")
    public AssetSummaryResponse getSummary() {
        return assetService.getSummary();
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody AssetRequest request) {
        return assetService.create(request);
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return assetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assetService.delete(id);
    }
}
