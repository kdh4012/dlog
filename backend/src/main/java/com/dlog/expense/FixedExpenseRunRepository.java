package com.dlog.expense;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedExpenseRunRepository extends JpaRepository<FixedExpenseRun, FixedExpenseRun.Key> {
}
