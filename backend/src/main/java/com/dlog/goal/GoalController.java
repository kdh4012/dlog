package com.dlog.goal;

import com.dlog.goal.dto.GoalDoneRequest;
import com.dlog.goal.dto.GoalOrderRequest;
import com.dlog.goal.dto.GoalRequest;
import com.dlog.goal.dto.GoalResponse;
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

/** 연도별 목표 API - SecurityConfig 에서 관리자 전용으로 제한 (나만 보기) */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /** year 생략 시 올해 */
    @GetMapping
    public List<GoalResponse> getAll(@RequestParam(required = false) Integer year) {
        return goalService.getAll(year != null ? year : GoalService.thisYear());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody GoalRequest request) {
        return goalService.create(request);
    }

    /** 순서 변경: 그 해 목표 id 전체를 순서대로 */
    @PutMapping("/order")
    public List<GoalResponse> reorder(@Valid @RequestBody GoalOrderRequest request) {
        return goalService.reorder(request.year(), request.ids());
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return goalService.update(id, request);
    }

    /** 달성 표시만 바꾼다 */
    @PatchMapping("/{id}/done")
    public GoalResponse changeDone(@PathVariable Long id, @Valid @RequestBody GoalDoneRequest request) {
        return goalService.changeDone(id, request.done());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        goalService.delete(id);
    }
}
