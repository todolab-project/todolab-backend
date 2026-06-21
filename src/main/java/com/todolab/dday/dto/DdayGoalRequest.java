package com.todolab.dday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DdayGoalRequest(
        @NotBlank
        @Size(max = 50)
        String title,

        @NotNull
        LocalDate targetDate
) {
}
