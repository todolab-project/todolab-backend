package com.todolab.task.dto;

import com.todolab.Constant;
import com.todolab.task.domain.TaskType;
import com.todolab.task.exception.TaskValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskRequest(
        @NotBlank(message = "제목은 필수값입니다")
        @Size(max = 30, message = "제목은 30자 이하여야 합니다")
        String title,

        @Size(max = 300, message = "설명은 300자 이하여야 합니다")
        String description,

        TaskType type,

        LocalDateTime startAt,
        LocalDateTime endAt,

        @Size(max = 30, message = "카테고리는 30자 이하여야 합니다")
        String category,

        boolean allDay
) {

    public TaskRequest(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String category,
            boolean allDay
    ) {
        this(title, description, TaskType.defaultType(), startAt, endAt, category, allDay);
    }

    public TaskType normalizedType() {
        return type == null ? TaskType.defaultType() : type;
    }

    public void validate() {
        validateEndAtWithoutStartAt();
        validateAllDayTime();
        validateDateTimeOrder();
        validateUnscheduledAllDay();
        validateCategory();
    }

    private void validateEndAtWithoutStartAt() {
        if (endAt != null && startAt == null) {
            throw new TaskValidationException("종료 시간만 설정할 수 없습니다. 시작 시간을 함께 설정해주세요.");
        }
    }

    private void validateAllDayTime() {
        if (!allDay) {
            return;
        }

        if (startAt != null && !isMidnight(startAt)) {
            throw new TaskValidationException("종일 일정은 시간을 입력할 수 없습니다. 시작일은 00:00 기준이어야 합니다.");
        }

        if (endAt != null && !isMidnight(endAt)) {
            throw new TaskValidationException("종일 일정은 시간을 입력할 수 없습니다. 종료일은 00:00 기준이어야 합니다.");
        }
    }

    private void validateDateTimeOrder() {
        if (startAt == null || endAt == null) {
            return;
        }

        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new TaskValidationException("종료 시간은 시작 시간 이후여야 합니다.");
        }
    }

    private void validateUnscheduledAllDay() {
        if (startAt == null && endAt == null && allDay) {
            throw new TaskValidationException("미정 일정에는 종일 설정을 할 수 없습니다.");
        }
    }

    private void validateCategory() {
        if (category == null) {
            return;
        }

        String normalizedCategory = category.trim();
        if (normalizedCategory.isEmpty()) {
            return;
        }

        if (Constant.UNCATEGORIZED.equals(normalizedCategory)) {
            throw new TaskValidationException("'미분류'는 시스템 예약어라서 카테고리명으로 사용할 수 없습니다.");
        }
    }

    private boolean isMidnight(LocalDateTime dt) {
        return dt.toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}
