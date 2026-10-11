package com.dlog.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, Long> {

    /** 금액 큰 순 (같으면 먼저 등록한 순) */
    List<FixedExpense> findAllByOrderByAmountDescIdAsc();
}
