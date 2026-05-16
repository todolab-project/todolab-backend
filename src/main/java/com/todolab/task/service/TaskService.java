package com.todolab.task.service;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskTxService taskTxService;
    private final TaskRepository taskRepository;
    private final TaskCategoryGrouper taskCategoryGrouper;

    public TaskResponse create(TaskRequest req) {
        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .type(req.normalizedType())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .allDay(req.allDay())
                .category(req.category())
                .build();

        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }

    public TaskResponse getTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        return TaskResponse.from(task);
    }

    public List<TaskResponse> getTasks(TaskQueryRequest request) {
        return findTasks(request);
    }

    public List<TaskCategoryGroupResponse> getGroupedTasks(TaskQueryRequest request) {
        return taskCategoryGrouper.group(findTasks(request));
    }

    public List<TaskResponse> getUnscheduledTasks() {
        return findUnscheduledTasks();
    }

    public List<TaskCategoryGroupResponse> getGroupedUnscheduledTasks() {
        return taskCategoryGrouper.group(findUnscheduledTasks());
    }

    public TaskResponse update(Long id, TaskRequest taskRequest) {
        Task updated = taskTxService.updateTx(id, taskRequest);
        return TaskResponse.from(updated);
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    private List<TaskResponse> findTasks(TaskQueryRequest request) {
        final TaskQueryType type = request.getType();
        final String strDate = request.getDate();

        DateRange range = type.calculate(strDate);

        return taskRepository.findByDateRangeAndType(range.getStart(), range.getEnd(), request.getTaskType())
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    private List<TaskResponse> findUnscheduledTasks() {
        return taskRepository.findUnscheduledTask().stream()
                .map(TaskResponse::from)
                .toList();
    }

}
