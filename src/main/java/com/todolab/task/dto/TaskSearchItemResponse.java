package com.todolab.task.dto;

import com.todolab.task.domain.query.TaskSearchDateSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Task 검색 항목")
public record TaskSearchItemResponse(
        @Schema(description = "Task 본문", requiredMode = Schema.RequiredMode.REQUIRED)
        TaskResponse task,
        @Schema(description = "검색 정렬/필터 기준 날짜. 기준 날짜가 없으면 null입니다.", example = "2026-07-22", nullable = true)
        LocalDate relevantDate,
        @Schema(description = "relevantDate 산출 출처", example = "TARGET_DATE", requiredMode = Schema.RequiredMode.REQUIRED)
        TaskSearchDateSource dateSource
) {
}
