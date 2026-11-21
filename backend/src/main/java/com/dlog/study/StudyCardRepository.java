package com.dlog.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyCardRepository extends JpaRepository<StudyCard, Long> {

    List<StudyCard> findAllByOrderByCategoryAscSortOrderAscIdAsc();

    List<StudyCard> findAllByCategoryOrderBySortOrderAscIdAsc(StudyCategory category);
}
