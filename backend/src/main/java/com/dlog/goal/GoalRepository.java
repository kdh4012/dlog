package com.dlog.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    /** 등록한 순 */
    List<Goal> findAllByYearOrderByIdAsc(int year);
}
