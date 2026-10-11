package com.dlog.worklog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    List<WorkLog> findAllByOrderByStartDateDescIdDesc();
}
