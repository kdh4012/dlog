package com.dlog.watchlist;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.watchlist.dto.WatchlistRequest;
import com.dlog.watchlist.dto.WatchlistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistRepository repository;
    private final StockQuoteService quoteService;

    public List<WatchlistResponse> getAll() {
        List<WatchlistStock> stocks = repository.findAllByOrderBySortOrderAscIdAsc();
        Map<String, StockQuote> quotes = quoteService.quotes(stocks.stream().map(WatchlistStock::getTicker).toList());
        return stocks.stream().map(s -> WatchlistResponse.of(s, quotes.get(s.getTicker()))).toList();
    }

    @Transactional
    public WatchlistResponse add(WatchlistRequest r) {
        String ticker = r.ticker().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByTicker(ticker)) {
            throw new BusinessException(ErrorCode.WATCHLIST_DUPLICATE);
        }
        StockQuote quote = quoteService.quote(ticker).orElse(null);
        // 이름을 비우면 시세 API 의 종목명으로 채운다 (시세를 못 구하면 비워 둠)
        String name = blankToNull(r.name());
        if (name == null && quote != null) {
            name = quote.name();
        }
        WatchlistStock saved = repository.save(new WatchlistStock(ticker, name, r.targetPrice(), blankToNull(r.memo()),
                repository.findMaxSortOrder() + 1));
        return WatchlistResponse.of(saved, quote);
    }

    @Transactional
    public WatchlistResponse update(Long id, WatchlistRequest r) {
        WatchlistStock stock = findById(id);
        stock.update(blankToNull(r.name()), r.targetPrice(), blankToNull(r.memo()));
        return WatchlistResponse.of(stock, quoteService.quote(stock.getTicker()).orElse(null));
    }

    /**
     * 순서 변경. ids 는 전체 관심 종목 id 를 원하는 순서대로 담아야 한다
     * (화면이 오래돼 목록이 달라졌으면 400 → 화면에서 다시 불러온다).
     */
    @Transactional
    public List<WatchlistResponse> reorder(List<Long> ids) {
        List<WatchlistStock> stocks = repository.findAll();
        Map<Long, WatchlistStock> byId = stocks.stream().collect(Collectors.toMap(WatchlistStock::getId, s -> s));
        if (ids.size() != stocks.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).changeOrder(i + 1);
        }
        return getAll();
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private WatchlistStock findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_NOT_FOUND));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
