package com.dlog.diary;

import com.dlog.diary.dto.DiaryRequest;
import com.dlog.diary.dto.DiaryResponse;
import com.dlog.diary.dto.DiarySummaryResponse;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository repository;

    public PageResponse<DiarySummaryResponse> getList(Pageable pageable) {
        return PageResponse.from(repository.findAllByOrderByEntryDateDescIdDesc(pageable).map(DiarySummaryResponse::from));
    }

    public DiaryResponse get(Long id) {
        return DiaryResponse.from(findById(id));
    }

    @Transactional
    public DiaryResponse create(DiaryRequest r) {
        return DiaryResponse.from(repository.save(new DiaryEntry(r.entryDate(), blankToNull(r.title()), r.content())));
    }

    @Transactional
    public DiaryResponse update(Long id, DiaryRequest r) {
        DiaryEntry entry = findById(id);
        entry.update(r.entryDate(), blankToNull(r.title()), r.content());
        return DiaryResponse.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private DiaryEntry findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
