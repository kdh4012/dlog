package com.dlog.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    List<FixedExpense> findAllByOrderByDayOfMonthAscIdAsc();
}
