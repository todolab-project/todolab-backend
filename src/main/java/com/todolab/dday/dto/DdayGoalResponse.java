package com.todolab.dday.dto;

import com.todolab.dday.domain.DdayGoal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record DdayGoalResponse(
        Long id,
        String title,
        LocalDate targetDate,
        long daysLeft,
        LocalDateTime createdAt
) {

    public static DdayGoalResponse from(DdayGoal goal) {
        return new DdayGoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getTargetDate(),
                ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate()),
                goal.getCreatedAt()
        );
    }
}
