package com.dlog.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerItem, Long> {

    /** 그 달의 수입·고정지출. 금액 큰 순 (같으면 먼저 등록한 순) */
    List<LedgerItem> findAllByLedgerMonthOrderByAmountDescIdAsc(String ledgerMonth);

    /** 부채 (월 구분 없음). 금액 큰 순 */
    List<LedgerItem> findAllByKindOrderByAmountDescIdAsc(LedgerKind kind);

    /** 월별·구분별 합계: [yyyy-MM, kind, 합계] */
    @Query("""
            select i.ledgerMonth, i.kind, sum(i.amount) from LedgerItem i
            where i.ledgerMonth between :from and :to
            group by i.ledgerMonth, i.kind""")
    List<Object[]> sumByMonth(@Param("from") String from, @Param("to") String to);
}
