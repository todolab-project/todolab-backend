package com.todolab.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 검증 에러
    INVALID_INPUT(HttpStatus.BAD_REQUEST, 10001, "값이 올바르지 않습니다."),
    REQUIRED_VALUE_MISSING(HttpStatus.BAD_REQUEST, 10002, "필수값이 없습니다."),

    // Task
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, 20001, "일정을 찾을 수 없습니다."),

    // D-Day
    DDAY_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, 30001, "D-Day 목표를 찾을 수 없습니다."),

    // 서버 내부 오류
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 99999, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
