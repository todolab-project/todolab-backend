package com.todolab.view.model;

import java.time.LocalDate;
import java.util.List;

public record MonthPageModel(
        LocalDate currentDate,
        LocalDate selectedDate,
        LocalDate monthStart,
        LocalDate monthEnd,
        String monthLabel,
        String monthRange,
        List<CalendarCell> monthDays,
        int monthTotalCount
) {
}
