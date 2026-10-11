package com.dlog.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** [from, to) 기간 지출. 최신 날짜, 최근 입력 순 */
    List<Expense> findAllBySpentOnGreaterThanEqualAndSpentOnLessThanOrderBySpentOnDescIdDesc(LocalDate from, LocalDate to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.spentOn >= :from and e.spentOn < :to")
    long sumAmount(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
