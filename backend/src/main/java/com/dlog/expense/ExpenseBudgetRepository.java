package com.dlog.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, String> {

    /** month 이하에서 가장 최근 예산 (yyyy-MM 문자열은 사전순 = 시간순) */
    Optional<ExpenseBudget> findFirstByMonthLessThanEqualOrderByMonthDesc(String month);
}
