package com.todolab.auth.dto;

import com.todolab.user.dto.UserResponse;

import java.time.LocalDateTime;

public record TokenResponse(
        String tokenType,
        String accessToken,
        LocalDateTime expiresAt,
        UserResponse user
) {
}
