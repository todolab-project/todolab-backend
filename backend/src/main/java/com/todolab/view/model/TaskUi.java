package com.todolab.view.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskUi(
        Long id,
        String title,
        String description,
        LocalDate date,
        LocalTime time,
        boolean allDay,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String category,
        String color
) {
}
