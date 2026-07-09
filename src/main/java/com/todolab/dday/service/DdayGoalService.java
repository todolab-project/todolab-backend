package com.todolab.dday.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.Task;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.repository.TaskRepository;
import com.todolab.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DdayGoalService {

    private final DdayGoalRepository ddayGoalRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public DdayGoalResponse create(DdayGoalRequest request) {
        return create(request, null);
    }

    @Transactional
    public DdayGoalResponse createForOwner(DdayGoalRequest request, User owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner는 필수입니다.");
        }
        return create(request, owner);
    }

    private DdayGoalResponse create(DdayGoalRequest request, User owner) {
        DdayGoal saved = ddayGoalRepository.save(new DdayGoal(request.title(), request.targetDate(), owner));
        return DdayGoalResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DdayGoalResponse> findAll() {
        return ddayGoalRepository.findAllByOrderByTargetDateAscIdAsc().stream()
                .map(DdayGoalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DdayGoalResponse> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return ddayGoalRepository.findByTargetDateBetweenOrderByTargetDateAscIdAsc(startDate, endDate).stream()
                .map(DdayGoalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findTasks(Long id) {
        if (!ddayGoalRepository.existsById(id)) {
            throw new DdayGoalNotFoundException(id);
        }

        return taskRepository.findByDdayGoalId(id).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!ddayGoalRepository.existsById(id)) {
            throw new DdayGoalNotFoundException(id);
        }
        taskRepository.findByDdayGoalId(id)
                .forEach(Task::disconnectDdayGoal);
        ddayGoalRepository.deleteById(id);
    }
}
