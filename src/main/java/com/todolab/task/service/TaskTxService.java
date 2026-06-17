package com.todolab.task.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TodayOrderDirection;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.exception.TaskValidationException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        assignLastTodayOrder(task, targetDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task moveToInboxTx(Long id) {
        Task task = findTask(id);
        task.moveToInbox();
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
        assignLastTodayOrder(task, targetDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task carryOverTx(Long id, LocalDate nextDate) {
        Task task = findTask(id);
        task.carryOverTo(nextDate);
        assignLastTodayOrder(task, nextDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task reorderTodayTx(Long id, LocalDate targetDate, TodayOrderDirection direction) {
        Task task = findTask(id);
        validateTodayOrderTarget(task, targetDate, direction);

        List<Task> tasks = taskRepository.findPlannedTasks(targetDate, targetDate.plusDays(1));
        int currentIndex = findTaskIndex(tasks, id);
        int nextIndex = direction == TodayOrderDirection.UP ? currentIndex - 1 : currentIndex + 1;
        if (nextIndex < 0 || nextIndex >= tasks.size()) {
            return task;
        }

        normalizeTodayOrder(tasks);
        Task target = tasks.get(currentIndex);
        Task neighbor = tasks.get(nextIndex);
        int targetOrder = target.getTodayOrder();
        target.assignTodayOrder(neighbor.getTodayOrder());
        neighbor.assignTodayOrder(targetOrder);
        taskRepository.saveAll(tasks);
        return target;
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

    private void assignLastTodayOrder(Task task, LocalDate targetDate) {
        Integer maxOrder = taskRepository.findMaxTodayOrder(targetDate);
        task.assignTodayOrder(maxOrder == null ? 0 : maxOrder + 1);
    }

    private void validateTodayOrderTarget(Task task, LocalDate targetDate, TodayOrderDirection direction) {
        if (targetDate == null) {
            throw new TaskValidationException("실행 순서를 변경할 날짜가 필요합니다.");
        }
        if (direction == null) {
            throw new TaskValidationException("실행 순서 변경 방향이 필요합니다.");
        }
        if (task.getStatus() != TaskStatus.TODAY || !targetDate.equals(task.getPlannedDate())) {
            throw new TaskValidationException("해당 날짜의 Today Task만 실행 순서를 변경할 수 있습니다.");
        }
    }

    private int findTaskIndex(List<Task> tasks, Long id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (id.equals(tasks.get(i).getId())) {
                return i;
            }
        }
        throw new TaskValidationException("해당 날짜의 Today 목록에서 Task를 찾을 수 없습니다.");
    }

    private void normalizeTodayOrder(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).assignTodayOrder(i);
        }
    }
}
