package com.todolab.task.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.Task;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskTxService {

    private final TaskRepository taskRepository;
    private final DdayGoalRepository ddayGoalRepository;

    @Transactional
    public Task updateTx(Long id, TaskRequest req) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.update(req.title(), req.description(), req.normalizedType(), req.startAt(), req.endAt(), req.allDay(), req.category());
        return taskRepository.save(task);
    }

    @Transactional
    public Task moveToTodayTx(Long id, LocalDate targetDate) {
        Task task = findTask(id);
        task.moveToToday(targetDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTx(Long id, LocalDateTime completedAt) {
        Task task = findTask(id);
        task.complete(completedAt);
        return taskRepository.save(task);
    }

    @Transactional
    public Task reopenTodayTx(Long id, LocalDate targetDate) {
        Task task = findTask(id);
        task.reopenToday(targetDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task carryOverTx(Long id, LocalDate nextDate) {
        Task task = findTask(id);
        task.carryOverTo(nextDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task setDeferReasonTx(Long id, DeferReason reason) {
        Task task = findTask(id);
        task.setDeferReason(reason);
        return taskRepository.save(task);
    }

    @Transactional
    public Task clearDeferReasonTx(Long id) {
        Task task = findTask(id);
        task.clearDeferReason();
        return taskRepository.save(task);
    }

    @Transactional
    public Task connectDdayGoalTx(Long id, Long ddayGoalId) {
        Task task = findTask(id);
        DdayGoal ddayGoal = ddayGoalRepository.findById(ddayGoalId)
                .orElseThrow(() -> new DdayGoalNotFoundException(ddayGoalId));

        task.connectDdayGoal(ddayGoal);
        return taskRepository.save(task);
    }

    @Transactional
    public Task disconnectDdayGoalTx(Long id) {
        Task task = findTask(id);
        task.disconnectDdayGoal();
        return taskRepository.save(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
