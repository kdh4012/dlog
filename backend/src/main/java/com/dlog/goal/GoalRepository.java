package com.dlog.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    /** 정한 순서대로 (같으면 먼저 등록한 순) */
    List<Goal> findAllByYearOrderBySortOrderAscIdAsc(int year);

    /** 새 목표는 그 해 맨 아래에 붙인다 */
    @Query("select coalesce(max(g.sortOrder), 0) from Goal g where g.year = :year")
    int findMaxSortOrder(@Param("year") int year);
}
