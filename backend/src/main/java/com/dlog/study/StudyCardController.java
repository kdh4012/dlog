package com.dlog.study;

import com.dlog.study.dto.MemorizedRequest;
import com.dlog.study.dto.StudyCardImportRequest;
import com.dlog.study.dto.StudyCardRequest;
import com.dlog.study.dto.StudyCardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 학습 노트 카드. /api/** 규칙에 따라 관리자만 접근한다 */
@RestController
@RequestMapping("/api/study-cards")
@RequiredArgsConstructor
public class StudyCardController {

    private final StudyCardService service;

    @GetMapping
    public List<StudyCardResponse> getAll(@RequestParam(required = false) StudyCategory category) {
        return service.getAll(category);
    }

    @GetMapping("/{id}")
    public StudyCardResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyCardResponse create(@Valid @RequestBody StudyCardRequest request) {
        return service.create(request);
    }

    /** JSON 파일 가져오기: 추가한 카드 수를 돌려준다 */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> importAll(@Valid @RequestBody StudyCardImportRequest request) {
        return Map.of("imported", service.importAll(request.cards()));
    }

    @PutMapping("/{id}")
    public StudyCardResponse update(@PathVariable Long id, @Valid @RequestBody StudyCardRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/memorized")
    public StudyCardResponse changeMemorized(@PathVariable Long id, @Valid @RequestBody MemorizedRequest request) {
        return service.changeMemorized(id, request.memorized());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
