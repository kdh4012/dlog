package com.dlog.study;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.study.dto.StudyCardRequest;
import com.dlog.study.dto.StudyCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyCardService {

    private final StudyCardRepository repository;

    /** category 가 null 이면 전체를 분류·순서대로 조회 */
    public List<StudyCardResponse> getAll(StudyCategory category) {
        List<StudyCard> cards = category == null
                ? repository.findAllByOrderByCategoryAscSortOrderAscIdAsc()
                : repository.findAllByCategoryOrderBySortOrderAscIdAsc(category);
        return cards.stream().map(StudyCardResponse::from).toList();
    }

    public StudyCardResponse get(Long id) {
        return StudyCardResponse.from(findById(id));
    }

    @Transactional
    public StudyCardResponse create(StudyCardRequest r) {
        StudyCard card = StudyCard.builder()
                .category(r.category())
                .types(r.types())
                .question(r.question())
                .answer(r.answer())
                .sortOrder(orderOf(r))
                .build();
        return StudyCardResponse.from(repository.save(card));
    }

    /** 전체를 한 트랜잭션으로 저장해, 하나라도 실패하면 아무것도 들어가지 않는다 */
    @Transactional
    public int importAll(List<StudyCardRequest> requests) {
        List<StudyCard> cards = requests.stream()
                .map(r -> StudyCard.builder()
                        .category(r.category())
                        .types(r.types())
                        .question(r.question())
                        .answer(r.answer())
                        .sortOrder(orderOf(r))
                        .build())
                .toList();
        return repository.saveAll(cards).size();
    }

    @Transactional
    public StudyCardResponse update(Long id, StudyCardRequest r) {
        StudyCard card = findById(id);
        card.update(r.category(), r.types(), r.question(), r.answer(), orderOf(r));
        return StudyCardResponse.from(card);
    }

    @Transactional
    public StudyCardResponse changeMemorized(Long id, boolean memorized) {
        StudyCard card = findById(id);
        card.changeMemorized(memorized);
        return StudyCardResponse.from(card);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private static int orderOf(StudyCardRequest r) {
        return r.sortOrder() == null ? 0 : r.sortOrder();
    }

    private StudyCard findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_CARD_NOT_FOUND));
    }
}
