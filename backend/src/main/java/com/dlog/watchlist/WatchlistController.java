package com.dlog.watchlist;

import com.dlog.watchlist.dto.WatchlistOrderRequest;
import com.dlog.watchlist.dto.WatchlistRequest;
import com.dlog.watchlist.dto.WatchlistResponse;
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

/** 관심 종목(미국 주식) API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistResponse> getAll() {
        return watchlistService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse add(@Valid @RequestBody WatchlistRequest request) {
        return watchlistService.add(request);
    }

    /** 순서 변경: 전체 id 를 원하는 순서대로 */
    @PutMapping("/order")
    public List<WatchlistResponse> reorder(@Valid @RequestBody WatchlistOrderRequest request) {
        return watchlistService.reorder(request.ids());
    }

    @PutMapping("/{id}")
    public WatchlistResponse update(@PathVariable Long id, @Valid @RequestBody WatchlistRequest request) {
        return watchlistService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        watchlistService.delete(id);
    }
}
