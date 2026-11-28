package com.dlog.asset.memo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 자산 포트폴리오 메모 API - /api/** 는 관리자 전용 */
@RestController
@RequestMapping("/api/assets/memo")
@RequiredArgsConstructor
public class AssetMemoController {

    private final AssetMemoService memoService;

    @GetMapping
    public AssetMemoResponse get() {
        return memoService.get();
    }

    @PutMapping
    public AssetMemoResponse save(@Valid @RequestBody AssetMemoRequest request) {
        return memoService.save(request.content());
    }
}
