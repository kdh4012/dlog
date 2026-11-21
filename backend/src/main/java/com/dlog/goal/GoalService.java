package com.dlog.goal;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.goal.dto.GoalRequest;
import com.dlog.goal.dto.GoalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GoalRepository repository;

    /** 올해 (한국 시간) */
    public static int thisYear() {
        return Year.now(KST).getValue();
    }

    public List<GoalResponse> getAll(int year) {
        return repository.findAllByYearOrderByIdAsc(year).stream().map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse create(GoalRequest r) {
        int year = r.year() != null ? r.year() : thisYear();
        return GoalResponse.from(repository.save(new Goal(year, r.title().trim())));
    }

    @Transactional
    public GoalResponse update(Long id, GoalRequest r) {
        Goal goal = findById(id);
        goal.changeTitle(r.title().trim());
        return GoalResponse.from(goal);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private Goal findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));
    }
}
