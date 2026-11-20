package com.dlog.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerMonthRepository extends JpaRepository<LedgerMonth, String> {

    /** ym 보다 앞선 달 중 가장 최근에 채운 달 (yyyy-MM 문자열은 사전순 = 시간순) */
    Optional<LedgerMonth> findTopByYmLessThanOrderByYmDesc(String ym);
}
