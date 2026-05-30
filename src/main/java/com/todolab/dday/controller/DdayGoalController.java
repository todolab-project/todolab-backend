package com.todolab.dday.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.service.DdayGoalService;
import com.todolab.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ddays")
@RequiredArgsConstructor
public class DdayGoalController {

    private final DdayGoalService ddayGoalService;

    @PostMapping
    public ResponseEntity<ApiResponse<DdayGoalResponse>> create(@Valid @RequestBody DdayGoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DdayGoalResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.findAll()));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> findTasks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ddayGoalService.findTasks(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DdayGoalResponse>> delete(@PathVariable Long id) {
        ddayGoalService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(new DdayGoalResponse(id, null, null, 0, null)));
    }
}
