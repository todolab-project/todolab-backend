package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskType;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepositoryCustom {
    List<Task> findByDateRange(LocalDateTime start, LocalDateTime end);

    List<Task> findByDateRangeAndType(LocalDateTime start, LocalDateTime end, TaskType taskType);

    List<Task> findUnscheduledTask();
}
