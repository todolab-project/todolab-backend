package com.todolab.dday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DdayGoalTaskRequest(
        @NotBlank(message = "제목은 필수값입니다")
        @Size(max = 30, message = "제목은 30자 이하여야 합니다")
        String title,

        @NotNull(message = "실행 날짜는 필수값입니다")
        LocalDate date
) {
}
