package com.dlog.diary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<DiaryEntry, Long> {

    /** 최근 날짜 순 (같은 날이면 나중에 쓴 순) */
    Page<DiaryEntry> findAllByOrderByEntryDateDescIdDesc(Pageable pageable);
}
