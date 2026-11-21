package com.dlog.goal.dto;

import com.dlog.goal.Goal;

public record GoalResponse(Long id, int year, String title, boolean done) {

    public static GoalResponse from(Goal g) {
        return new GoalResponse(g.getId(), g.getYear(), g.getTitle(), g.isDone());
    }
}
