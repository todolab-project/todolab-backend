package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepositoryCustom {
    List<Task> findByDateRange(LocalDateTime start, LocalDateTime end);

    List<Task> findByDateRangeAndType(LocalDateTime start, LocalDateTime end, TaskType taskType);

    List<Task> findUnscheduledTask();

    List<Task> findByStatus(TaskStatus status);

    List<Task> findPlannedTasks(LocalDate fromInclusive, LocalDate toExclusive);

    Integer findMaxTodayOrder(LocalDate targetDate);

    List<Task> findDoneTasks(LocalDate completedDate);

    List<Task> findDoneTasksBetween(LocalDate startDate, LocalDate endDate);

    List<Task> findByDdayGoalId(Long ddayGoalId);
}
