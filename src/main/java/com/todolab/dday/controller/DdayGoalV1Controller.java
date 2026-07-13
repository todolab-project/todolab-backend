package com.todolab.dday.controller;

import com.todolab.auth.service.CurrentUserService;
import com.todolab.common.api.ApiResponse;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.dto.DdayGoalTaskRequest;
import com.todolab.dday.service.DdayGoalService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todolab.task.dto.TaskResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dday-goals")
@RequiredArgsConstructor
public class DdayGoalV1Controller {

    private final DdayGoalService ddayGoalService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<DdayGoalResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DdayGoalRequest request
    ) {
        User owner = currentUserService.requireUser(jwt);
        DdayGoalResponse response = ddayGoalService.createForOwner(request, owner);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DdayGoalResponse>>> findAll(@AuthenticationPrincipal Jwt jwt) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.findAllForOwner(owner)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DdayGoalResponse>> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.getForOwner(id, owner)));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> findTasks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.findTasksForOwner(id, owner)));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTodayTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody DdayGoalTaskRequest request
    ) {
        User owner = currentUserService.requireUser(jwt);
        TaskResponse response = taskService.createTodayTaskForDdayGoalForOwner(
                id,
                request.title(),
                request.date(),
                owner
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        User owner = currentUserService.requireUser(jwt);
        ddayGoalService.deleteForOwner(id, owner);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
