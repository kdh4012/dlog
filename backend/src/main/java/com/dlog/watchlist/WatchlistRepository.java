package com.dlog.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistStock, Long> {

    boolean existsByTicker(String ticker);

    /** 먼저 담은 순 */
    List<WatchlistStock> findAllByOrderByIdAsc();
}
