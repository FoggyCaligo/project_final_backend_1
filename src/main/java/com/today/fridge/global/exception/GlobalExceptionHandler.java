package com.today.fridge.global.exception;

import com.today.fridge.global.filter.RequestIdFilter;
import com.today.fridge.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                ex.getCode(), ex.getMessage(), null, requestId);
        return ResponseEntity.status(ex.getStatus()).body(body);
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
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
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

    /**
     * 매핑 없는 경로(예: {@code GET /}, {@code /favicon.ico})는 기본적으로 이 예외가 나며,
     * {@link Exception} 핸들러에 떨어지면 500으로 보이므로 404로 분리한다.
     */
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
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                null,
                requestId);
        return ResponseEntity.internalServerError().body(body);
    }
}
