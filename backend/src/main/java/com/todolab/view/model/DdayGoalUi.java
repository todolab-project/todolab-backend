package com.todolab.view.model;

import java.time.LocalDate;

public record DdayGoalUi(
        Long id,
        String title,
        LocalDate targetDate,
        long daysLeft
) {

    public String label() {
        if (daysLeft == 0) {
            return "D-Day";
        }
        if (daysLeft > 0) {
            return "D-" + daysLeft;
        }
        return "D+" + Math.abs(daysLeft);
    }
}
