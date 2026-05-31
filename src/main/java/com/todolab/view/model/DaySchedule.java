package com.todolab.view.model;

import java.time.LocalDate;
import java.util.List;

public record DaySchedule(
        LocalDate date,
        String dayLabel,
        List<TaskUi> tasks,
        List<DdayGoalUi> ddayGoals
) {
}
