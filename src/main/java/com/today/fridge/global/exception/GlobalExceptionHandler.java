package com.today.fridge.global.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.today.fridge.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
