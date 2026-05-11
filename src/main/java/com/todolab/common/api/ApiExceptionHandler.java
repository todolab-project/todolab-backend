package com.todolab.common.api;

import com.todolab.task.exception.TaskValidationException;
import com.todolab.task.exception.TaskNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Bean Validation 에러 처리 (MVC)
     * - @RequestBody @Valid  -> MethodArgumentNotValidException
     * - @ModelAttribute / 바인딩 -> BindException
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<?>> handleValidationException(Exception e) {
        FieldError error = extractFirstFieldError(e);
        if (error != null) {
            String detail = error.getField() + ": " + error.getDefaultMessage();
            log.error("Validation Failed : {}", detail);
        } else {
            log.error("Validation Failed : {}", e.getMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    /**
     * PathVariable / RequestParam 타입 미스매치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Type Mismatch : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    /**
     * 필수 요청값 누락 (@RequestParam, @PathVariable)
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingPathVariableException.class})
    public ResponseEntity<ApiResponse<?>> handleMissingRequestValueException(Exception e) {
        log.error("Missing Request Value : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    /**
     * JSON 파싱 오류 등
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("Unreadable Request Body : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    /**
     * 도메인 커스텀 검증
     */
    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleTaskValidationException(TaskValidationException e) {
        log.error("Task Validation Failed : {}", e.getDetail());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    /**
     * Task 리소스 없음
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleTaskNotFoundException(TaskNotFoundException e) {
        log.warn("Task Not Found : {}", e.getDetail());
        return ResponseEntity.status(ErrorCode.TASK_NOT_FOUND.getStatus())
                .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }

    private FieldError extractFirstFieldError(Exception e) {
        if (e instanceof MethodArgumentNotValidException manve) {
            return manve.getBindingResult().getFieldErrors().isEmpty()
                    ? null
                    : manve.getBindingResult().getFieldErrors().getFirst();
        }
        if (e instanceof BindException be) {
            return be.getBindingResult().getFieldErrors().isEmpty()
                    ? null
                    : be.getBindingResult().getFieldErrors().getFirst();
        }
        return null;
    }
}
