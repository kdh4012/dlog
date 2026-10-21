package com.dlog.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistStock, Long> {

    boolean existsByTicker(String ticker);

    /** 사용자가 정한 순서 (같으면 먼저 담은 순) */
    List<WatchlistStock> findAllByOrderBySortOrderAscIdAsc();

    /** 새 종목은 맨 아래에 붙인다 */
    @Query("select coalesce(max(w.sortOrder), 0) from WatchlistStock w")
    int findMaxSortOrder();
}
