package com.todolab.task.dto;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TaskResponse(
        Long id,
        TaskType type,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        boolean unscheduled,
        String category,
        LocalDateTime createdAt
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
        this(id, TaskType.defaultType(), title, description, startAt, endAt, allDay, unscheduled, category, createdAt);
    }

    public static TaskResponse from(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .title(t.getTitle())
                .description(t.getDescription())
                .startAt(t.getStartAt())
                .endAt(t.getEndAt())
                .allDay(t.isAllDay())
                .unscheduled(t.isUnscheduled())
                .category(t.getCategory())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
