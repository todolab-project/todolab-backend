package com.todolab.view.model;

import java.time.LocalDate;
import java.util.List;

public record WeekPageModel(
        LocalDate currentDate,
        LocalDate weekStart,
        LocalDate weekEnd,
        String weekRange,
        LocalDate selectedDate,
        List<DaySchedule> weeklyTasks,
        DaySchedule selectedSchedule,
        int weekTotalCount
) {
}
