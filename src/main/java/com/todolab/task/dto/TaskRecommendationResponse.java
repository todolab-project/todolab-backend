package com.todolab.task.dto;

public record TaskRecommendationResponse(
        TaskResponse task,
        String reason
) {
}
