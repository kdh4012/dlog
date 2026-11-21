package com.dlog.diary;

import com.dlog.diary.dto.DiaryRequest;
import com.dlog.diary.dto.DiaryResponse;
import com.dlog.diary.dto.DiarySummaryResponse;
import com.dlog.global.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 일기 API - SecurityConfig 에서 관리자 전용으로 제한 (나만 보기) */
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /** 최근 날짜 순. 정렬은 고정이라 page·size 만 받는다 */
    @GetMapping
    public PageResponse<DiarySummaryResponse> getList(@PageableDefault(size = 20) Pageable pageable) {
        return diaryService.getList(pageable);
    }

    @GetMapping("/{id}")
    public DiaryResponse get(@PathVariable Long id) {
        return diaryService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiaryResponse create(@Valid @RequestBody DiaryRequest request) {
        return diaryService.create(request);
    }

    @PutMapping("/{id}")
    public DiaryResponse update(@PathVariable Long id, @Valid @RequestBody DiaryRequest request) {
        return diaryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        diaryService.delete(id);
    }
}
