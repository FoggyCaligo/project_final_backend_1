package com.today.fridge.global.exception;

import com.today.fridge.global.filter.RequestIdFilter;
import com.today.fridge.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                ex.getCode(), ex.getMessage(), null, requestId);
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(ExceptionTemplate.class)
    public ResponseEntity<ApiResponse<Object>> handleExceptionTemplate(
            ExceptionTemplate ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        log.error("예외가 발생했습니다 : {}", ex.getMessage(), ex);
        ApiResponse<Object> body = ApiResponse.fail(
                ex.getErrorCode().name(), ex.getMessage(), null, requestId);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorEntry)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                data,
                requestId);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, String> toErrorEntry(FieldError fe) {
        Map<String, String> m = new HashMap<>();
        m.put("field", fe.getField());
        m.put("reason", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        return m;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        Map<String, Object> data = new HashMap<>();
        data.put("path", request.getRequestURI());
        data.put("method", ex.getHttpMethod());
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", data, requestId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAny(Exception ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        log.error("Unhandled exception requestId={} path={}", requestId, request.getRequestURI(), ex);
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                null,
                requestId);
        return ResponseEntity.internalServerError().body(body);
    }
}
