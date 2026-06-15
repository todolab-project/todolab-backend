package com.todolab.task.dto;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.ScheduleSource;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TaskType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Builder
public record TaskResponse(
        Long id,
        TaskType type,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        ScheduleSource scheduleSource,
        boolean unscheduled,
        String category,
        TaskStatus status,
        LocalDate plannedDate,
        LocalDate targetDate,
        LocalDateTime completedAt,
        int carryOverCount,
        boolean staleCarryOver,
        DeferReason deferReason,
        String deferReasonLabel,
        Long ddayGoalId,
        String ddayGoalTitle,
        LocalDate ddayGoalTargetDate,
        Long ddayDaysLeft,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public TaskResponse(
            Long id,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            boolean unscheduled,
            String category,
            LocalDateTime createdAt
    ) {
        this(id, TaskType.defaultType(), title, description, startAt, endAt, allDay, startAt == null && endAt == null ? null : ScheduleSource.USER, unscheduled, category, null, null, null, null, 0, false, null, null, null, null, null, null, createdAt, null);
    }

    public static TaskResponse from(Task t) {
        var ddayGoal = t.getDdayGoal();
        return TaskResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .title(t.getTitle())
                .description(t.getDescription())
                .startAt(t.getStartAt())
                .endAt(t.getEndAt())
                .allDay(t.isAllDay())
                .scheduleSource(t.getScheduleSource())
                .unscheduled(t.isUnscheduled())
                .category(t.getCategory())
                .status(t.getStatus())
                .plannedDate(t.getPlannedDate())
                .targetDate(t.getTargetDate())
                .completedAt(t.getCompletedAt())
                .carryOverCount(t.getCarryOverCount())
                .staleCarryOver(t.isStaleCarryOver())
                .deferReason(t.getDeferReason())
                .deferReasonLabel(t.getDeferReason() == null ? null : t.getDeferReason().getLabel())
                .ddayGoalId(ddayGoal == null ? null : ddayGoal.getId())
                .ddayGoalTitle(ddayGoal == null ? null : ddayGoal.getTitle())
                .ddayGoalTargetDate(ddayGoal == null ? null : ddayGoal.getTargetDate())
                .ddayDaysLeft(ddayGoal == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), ddayGoal.getTargetDate()))
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
