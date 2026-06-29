package com.todolab.auth.dto;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        String role
) {
}
