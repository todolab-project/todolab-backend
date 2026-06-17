package com.todolab.task.service;

import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRecommendationResponse;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

    public List<TaskResponse> getInboxTasks() {
        return taskRepository.findByStatus(TaskStatus.INBOX).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<TaskRecommendationResponse> getTodayRecommendations(LocalDate referenceDate) {
        List<TaskResponse> overdueTasks = taskRepository.findPlannedTasks(null, referenceDate).stream()
                .map(TaskResponse::from)
                .toList();
        List<TaskResponse> inboxTasks = taskRepository.findByStatus(TaskStatus.INBOX).stream()
                .map(TaskResponse::from)
                .toList();

        return java.util.stream.Stream.concat(overdueTasks.stream(), inboxTasks.stream())
                .map(task -> RecommendationCandidate.from(task, referenceDate))
                .sorted(Comparator
                        .comparingInt(RecommendationCandidate::priority)
                        .thenComparingLong(RecommendationCandidate::sortKey)
                        .thenComparing(candidate -> candidate.task().id(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(candidate -> new TaskRecommendationResponse(candidate.task(), candidate.reason()))
                .toList();
    }

    public List<TaskResponse> getTodayTasks(LocalDate targetDate) {
        return taskRepository.findPlannedTasks(targetDate, targetDate.plusDays(1)).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<TaskResponse> getPlannedTasksBetween(LocalDate startDate, LocalDate endDate) {
        return taskRepository.findPlannedTasks(startDate, endDate.plusDays(1)).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<TaskResponse> getOverdueTasks(LocalDate beforeDate) {
        return taskRepository.findPlannedTasks(null, beforeDate).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<TaskResponse> getDoneTasks(LocalDate completedDate) {
        return taskRepository.findDoneTasks(completedDate).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public List<TaskResponse> getDoneTasksBetween(LocalDate startDate, LocalDate endDate) {
        return taskRepository.findDoneTasksBetween(startDate, endDate).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse update(Long id, TaskRequest taskRequest) {
        Task updated = taskTxService.updateTx(id, taskRequest);
        return TaskResponse.from(updated);
    }

    public TaskResponse moveToToday(Long id, LocalDate targetDate) {
        Task moved = taskTxService.moveToTodayTx(id, targetDate);
        return TaskResponse.from(moved);
    }

    public TaskResponse moveToInbox(Long id) {
        Task moved = taskTxService.moveToInboxTx(id);
        return TaskResponse.from(moved);
    }

    public TaskResponse complete(Long id, LocalDateTime completedAt) {
        Task completed = taskTxService.completeTx(id, completedAt);
        return TaskResponse.from(completed);
    }

    public TaskResponse reopenToday(Long id, LocalDate targetDate) {
        Task reopened = taskTxService.reopenTodayTx(id, targetDate);
        return TaskResponse.from(reopened);
    }

    public TaskResponse carryOver(Long id, LocalDate nextDate) {
        Task carriedOver = taskTxService.carryOverTx(id, nextDate);
        return TaskResponse.from(carriedOver);
    }

    public TaskResponse setDeferReason(Long id, DeferReason reason) {
        Task updated = taskTxService.setDeferReasonTx(id, reason);
        return TaskResponse.from(updated);
    }

    public TaskResponse clearDeferReason(Long id) {
        Task updated = taskTxService.clearDeferReasonTx(id);
        return TaskResponse.from(updated);
    }

    public TaskResponse connectDdayGoal(Long id, Long ddayGoalId) {
        Task connected = taskTxService.connectDdayGoalTx(id, ddayGoalId);
        return TaskResponse.from(connected);
    }

    public TaskResponse disconnectDdayGoal(Long id) {
        Task disconnected = taskTxService.disconnectDdayGoalTx(id);
        return TaskResponse.from(disconnected);
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

    private record RecommendationCandidate(TaskResponse task, String reason, int priority, long sortKey) {
        static RecommendationCandidate from(TaskResponse task, LocalDate referenceDate) {
            if (task.carryOverCount() >= 3) {
                return new RecommendationCandidate(task, "다시 정리 필요", 0, -task.carryOverCount());
            }

            LocalDate plannedDate = task.plannedDate();
            if (task.status() == TaskStatus.TODAY && plannedDate != null && plannedDate.isBefore(referenceDate)) {
                long overdueDays = ChronoUnit.DAYS.between(plannedDate, referenceDate);
                return new RecommendationCandidate(task, "지난 미완료", 1, -overdueDays);
            }

            LocalDate ddayDate = task.ddayGoalTargetDate();
            if (ddayDate != null && !ddayDate.isBefore(referenceDate)) {
                long daysLeft = ChronoUnit.DAYS.between(referenceDate, ddayDate);
                if (!ddayDate.isAfter(referenceDate.plusDays(3))) {
                    return new RecommendationCandidate(task, "D-Day 3일 이내", 2, daysLeft);
                }
                if (!ddayDate.isAfter(referenceDate.plusDays(14))) {
                    return new RecommendationCandidate(task, "D-Day 임박", 3, daysLeft);
                }
            }

            LocalDateTime createdAt = task.createdAt();
            if (createdAt != null && !createdAt.toLocalDate().isAfter(referenceDate.minusDays(7))) {
                return new RecommendationCandidate(task, "오래 기록", 4, createdAtSortKey(createdAt));
            }

            return new RecommendationCandidate(
                    task,
                    "최근 기록",
                    5,
                    createdAt == null ? Long.MAX_VALUE : -createdAtSortKey(createdAt)
            );
        }

        private static long createdAtSortKey(LocalDateTime createdAt) {
            return createdAt.toLocalDate().toEpochDay() * 86_400L + createdAt.toLocalTime().toSecondOfDay();
        }
    }

}
