package com.todolab.auth.controller;

import com.todolab.auth.dto.AuthenticatedUserResponse;
import com.todolab.auth.dto.RegisterRequest;
import com.todolab.auth.dto.LoginRequest;
import com.todolab.auth.dto.TokenResponse;
import com.todolab.common.api.ApiResponse;
import com.todolab.auth.service.AuthService;
import com.todolab.user.dto.UserResponse;
import com.todolab.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthenticatedUserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserResponse response = new AuthenticatedUserResponse(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("role")
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
