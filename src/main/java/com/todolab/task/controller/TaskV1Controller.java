package com.todolab.task.controller;

import com.todolab.auth.service.CurrentUserService;
import com.todolab.common.api.ApiResponse;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.TodayOrderDirection;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRecommendationResponse;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import com.todolab.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskV1Controller {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskRequest request
    ) {
        request.validate();
        User owner = currentUserService.requireUser(jwt);
        TaskResponse response = taskService.createForOwner(request, owner);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskForOwner(id, owner)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String date
    ) {
        User owner = currentUserService.requireUser(jwt);
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawTaskType(taskType)
                .rawDate(date)
                .build();

        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksForOwner(request, owner)));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getInboxTasks(@AuthenticationPrincipal Jwt jwt) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getInboxTasksForOwner(owner)));
    }

    @GetMapping({"/recommendations/today", "/today/recommendations"})
    public ResponseEntity<ApiResponse<List<TaskRecommendationResponse>>> getTodayRecommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getTodayRecommendationsForOwner(date, owner)));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTodayTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getTodayTasksForOwner(date, owner)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getOverdueTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getOverdueTasksForOwner(date, owner)));
    }

    @GetMapping("/stale")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getStaleTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        LocalDate referenceDate = date == null ? LocalDate.now() : date;
        return ResponseEntity.ok(ApiResponse.success(taskService.getOverdueTasksForOwner(referenceDate, owner)));
    }

    @GetMapping("/done")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getDoneTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getDoneTasksForOwner(date, owner)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        request.validate();
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.updateForOwner(id, request, owner)));
    }

    @PatchMapping("/{id}/today")
    public ResponseEntity<ApiResponse<TaskResponse>> moveToToday(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.moveToTodayForOwner(id, date, owner)));
    }

    @PatchMapping("/{id}/inbox")
    public ResponseEntity<ApiResponse<TaskResponse>> moveToInbox(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.moveToInboxForOwner(id, owner)));
    }

    @PatchMapping("/{id}/done")
    public ResponseEntity<ApiResponse<TaskResponse>> complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam(required = false) LocalDateTime completedAt
    ) {
        User owner = currentUserService.requireUser(jwt);
        LocalDateTime effectiveCompletedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        return ResponseEntity.ok(ApiResponse.success(taskService.completeForOwner(id, effectiveCompletedAt, owner)));
    }

    @PatchMapping("/{id}/done/cancel")
    public ResponseEntity<ApiResponse<TaskResponse>> reopenToday(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.reopenTodayForOwner(id, date, owner)));
    }

    @PatchMapping("/{id}/carry-over")
    public ResponseEntity<ApiResponse<TaskResponse>> carryOver(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.carryOverForOwner(id, date, owner)));
    }

    @PatchMapping("/{id}/today-order")
    public ResponseEntity<ApiResponse<TaskResponse>> reorderToday(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam LocalDate date,
            @RequestParam TodayOrderDirection direction
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.reorderTodayForOwner(id, date, direction, owner)));
    }

    @PatchMapping("/{id}/defer-reason")
    public ResponseEntity<ApiResponse<TaskResponse>> setDeferReason(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam DeferReason reason
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.setDeferReasonForOwner(id, reason, owner)));
    }

    @DeleteMapping("/{id}/defer-reason")
    public ResponseEntity<ApiResponse<TaskResponse>> clearDeferReason(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.clearDeferReasonForOwner(id, owner)));
    }

    @PatchMapping("/{id}/dday-goal")
    public ResponseEntity<ApiResponse<TaskResponse>> connectDdayGoal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam Long ddayGoalId
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.connectDdayGoalForOwner(id, ddayGoalId, owner)));
    }

    @DeleteMapping("/{id}/dday-goal")
    public ResponseEntity<ApiResponse<TaskResponse>> disconnectDdayGoal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.disconnectDdayGoalForOwner(id, owner)));
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getUnscheduledTasks(@AuthenticationPrincipal Jwt jwt) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(taskService.getUnscheduledTasksForOwner(owner)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        taskService.deleteForOwner(id, owner);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
