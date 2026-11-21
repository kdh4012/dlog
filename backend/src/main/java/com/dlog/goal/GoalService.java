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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return repository.findAllByYearOrderBySortOrderAscIdAsc(year).stream().map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse create(GoalRequest r) {
        int year = r.year() != null ? r.year() : thisYear();
        return GoalResponse.from(repository.save(new Goal(year, r.title().trim(), repository.findMaxSortOrder(year) + 1)));
    }

    @Transactional
    public GoalResponse update(Long id, GoalRequest r) {
        Goal goal = findById(id);
        goal.changeTitle(r.title().trim());
        return GoalResponse.from(goal);
    }

    @Transactional
    public GoalResponse changeDone(Long id, boolean done) {
        Goal goal = findById(id);
        goal.changeDone(done);
        return GoalResponse.from(goal);
    }

    /**
     * 순서 변경. ids 는 그 해 목표 id 전체를 원하는 순서대로 담아야 한다
     * (화면이 오래돼 목록이 달라졌으면 400 → 화면에서 다시 불러온다).
     */
    @Transactional
    public List<GoalResponse> reorder(int year, List<Long> ids) {
        List<Goal> goals = repository.findAllByYearOrderBySortOrderAscIdAsc(year);
        Map<Long, Goal> byId = goals.stream().collect(Collectors.toMap(Goal::getId, g -> g));
        if (ids.size() != goals.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).changeOrder(i + 1);
        }
        return getAll(year);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private Goal findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));
    }
}
