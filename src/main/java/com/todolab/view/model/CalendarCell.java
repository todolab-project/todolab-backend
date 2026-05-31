package com.todolab.view.model;

import java.time.LocalDate;
import java.util.List;

public record CalendarCell(
        LocalDate date,
        boolean inMonth,
        List<TaskUi> tasks,
        List<DdayGoalUi> ddayGoals
) {
}
