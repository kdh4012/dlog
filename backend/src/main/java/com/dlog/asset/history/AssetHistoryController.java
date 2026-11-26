package com.dlog.asset.history;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 일별 총 자산 추이 - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/assets/history")
@RequiredArgsConstructor
public class AssetHistoryController {

    private final AssetHistoryService historyService;

    /** 오래된 날짜부터 전체 (하루 한 줄이라 몇 년치여도 작다) */
    @GetMapping
    public List<AssetHistoryResponse> getAll() {
        return historyService.getAll();
    }
}
