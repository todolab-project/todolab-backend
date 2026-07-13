package com.todolab.user.dto;

import com.todolab.user.domain.User;
import com.todolab.user.domain.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
