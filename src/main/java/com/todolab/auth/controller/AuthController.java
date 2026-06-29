package com.todolab.auth.controller;

import com.todolab.auth.dto.RegisterRequest;
import com.todolab.common.api.ApiResponse;
import com.todolab.user.dto.UserResponse;
import com.todolab.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
