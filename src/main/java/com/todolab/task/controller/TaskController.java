package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request) {
        log.info("[API] createTask request :: title={}, category={}, allDay={}",
                request.title(), request.category(), request.allDay());

        request.validate();

        TaskResponse res = taskService.create(request);

        log.info("[API] createTask success :: id={}", res.id());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        log.info("[API] getTask request :: id={}", id);
        TaskResponse res = taskService.getTask(id);
        log.info("[API] getTask success :: id={}", id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String date
    ) {
        log.info("[API] getTasks :: Type : {}, TaskType : {}, Date : {}", type, taskType, date);
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawTaskType(taskType)
                .rawDate(date)
                .build();

        List<TaskResponse> res = taskService.getTasks(request);
        log.info("[API] getTasks success :: type={}, taskType={}, date={}, taskCount={}", type, taskType, date, res.size());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<List<TaskCategoryGroupResponse>>> getGroupedTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String date
    ) {
        log.info("[API] getGroupedTasks :: Type : {}, TaskType : {}, Date : {}", type, taskType, date);
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawTaskType(taskType)
                .rawDate(date)
                .build();

        List<TaskCategoryGroupResponse> res = taskService.getGroupedTasks(request);
        log.info("[API] getGroupedTasks success :: type={}, taskType={}, date={}, groupCount={}", type, taskType, date, res.size());
        log.debug("[API] getGroupedTasks categories :: {}",
                res.stream().map(TaskCategoryGroupResponse::category).toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getInboxTasks() {
        log.info("[API] getInboxTasks request");

        List<TaskResponse> res = taskService.getInboxTasks();

        log.info("[API] getInboxTasks success :: taskCount={}", res.size());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTodayTasks(
            @RequestParam LocalDate date
    ) {
        log.info("[API] getTodayTasks request :: date={}", date);

        List<TaskResponse> res = taskService.getTodayTasks(date);

        log.info("[API] getTodayTasks success :: date={}, taskCount={}", date, res.size());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/done")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getDoneTasks(
            @RequestParam LocalDate date
    ) {
        log.info("[API] getDoneTasks request :: date={}", date);

        List<TaskResponse> res = taskService.getDoneTasks(date);

        log.info("[API] getDoneTasks success :: date={}, taskCount={}", date, res.size());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        log.info("[API] updateTask request :: id={}, title={}, category={}, allDay={}",
                id, request.title(), request.category(), request.allDay());

        request.validate();

        TaskResponse res = taskService.update(id, request);
        log.info("[API] updateTask success :: id={}", id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PatchMapping("/{id}/today")
    public ResponseEntity<ApiResponse<TaskResponse>> moveToToday(
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        log.info("[API] moveToToday request :: id={}, date={}", id, date);

        TaskResponse res = taskService.moveToToday(id, date);

        log.info("[API] moveToToday success :: id={}, date={}", id, date);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PatchMapping("/{id}/done")
    public ResponseEntity<ApiResponse<TaskResponse>> complete(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDateTime completedAt
    ) {
        LocalDateTime effectiveCompletedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        log.info("[API] complete request :: id={}, completedAt={}", id, effectiveCompletedAt);

        TaskResponse res = taskService.complete(id, effectiveCompletedAt);

        log.info("[API] complete success :: id={}, completedAt={}", id, effectiveCompletedAt);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PatchMapping("/{id}/carry-over")
    public ResponseEntity<ApiResponse<TaskResponse>> carryOver(
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        log.info("[API] carryOver request :: id={}, date={}", id, date);

        TaskResponse res = taskService.carryOver(id, date);

        log.info("[API] carryOver success :: id={}, date={}", id, date);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PatchMapping("/{id}/dday-goal")
    public ResponseEntity<ApiResponse<TaskResponse>> connectDdayGoal(
            @PathVariable Long id,
            @RequestParam Long ddayGoalId
    ) {
        log.info("[API] connectDdayGoal request :: id={}, ddayGoalId={}", id, ddayGoalId);

        TaskResponse res = taskService.connectDdayGoal(id, ddayGoalId);

        log.info("[API] connectDdayGoal success :: id={}, ddayGoalId={}", id, ddayGoalId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @DeleteMapping("/{id}/dday-goal")
    public ResponseEntity<ApiResponse<TaskResponse>> disconnectDdayGoal(@PathVariable Long id) {
        log.info("[API] disconnectDdayGoal request :: id={}", id);

        TaskResponse res = taskService.disconnectDdayGoal(id);

        log.info("[API] disconnectDdayGoal success :: id={}", id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getUnscheduledTasks() {
        log.info("[API] getUnscheduledTasks request");

        List<TaskResponse> res = taskService.getUnscheduledTasks();

        log.info("[API] getUnscheduledTasks success :: taskCount={}", res.size());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/unscheduled/grouped")
    public ResponseEntity<ApiResponse<List<TaskCategoryGroupResponse>>> getGroupedUnscheduledTasks() {
        log.info("[API] getGroupedUnscheduledTasks request");

        List<TaskCategoryGroupResponse> res = taskService.getGroupedUnscheduledTasks();

        log.info("[API] getGroupedUnscheduledTasks success :: groupCount={}", res.size());
        log.debug("[API] getGroupedUnscheduledTasks categories :: {}",
                res.stream().map(TaskCategoryGroupResponse::category).toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> deleteTask(@PathVariable Long id) {
        log.info("[API] deleteTask request :: id={}", id);

        taskService.delete(id);
        log.info("[API] deleteTask success :: id={}", id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(TaskResponse.builder().id(id).build()));
    }
}
