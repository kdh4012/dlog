package com.dlog.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerItem, Long> {

    /** 금액 큰 순 (같으면 먼저 등록한 순) */
    List<LedgerItem> findAllByOrderByAmountDescIdAsc();
}
