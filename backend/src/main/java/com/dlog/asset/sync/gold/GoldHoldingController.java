package com.dlog.asset.sync.gold;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 금 현물 보유량 입력 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/assets/gold")
@RequiredArgsConstructor
public class GoldHoldingController {

    private final GoldHoldingService goldHoldingService;

    @GetMapping
    public GoldHoldingResponse get() {
        return goldHoldingService.get();
    }

    @PutMapping
    public GoldHoldingResponse save(@Valid @RequestBody GoldHoldingRequest request) {
        return goldHoldingService.save(request);
    }
}
