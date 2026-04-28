package com.today.fridge.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.today.fridge.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : ErrorCode.VALIDATION_ERROR.getMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ExceptionTemplate.class)
    public <T> ApiResponse<T> handleException(ExceptionTemplate e) {
        log.error("예외가 발생했습니다 : {}", e.getMessage(), e);
        return ApiResponse.error(e.getErrorCode().name(), e.getMessage(), e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public <T> ApiResponse<T> handleInternalServerException(Exception e) {
        log.error("서버 내부 오류가 발생했습니다 : {}", e.getMessage(), e);
        return ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
