package com.today.fridge.global.exception;

import com.today.fridge.global.filter.RequestIdFilter;
import com.today.fridge.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                "VALIDATION_ERROR", "입력값이 올바르지 않습니다.", data, requestId);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, String> toErrorEntry(FieldError fe) {
        Map<String, String> m = new HashMap<>();
        m.put("field", fe.getField());
        m.put("reason", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        return m;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAny(Exception ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        ApiResponse<Map<String, Object>> body = ApiResponse.fail(
                "INTERNAL_ERROR", "서버 오류가 발생했습니다.", null, requestId);
        return ResponseEntity.internalServerError().body(body);
    }
}
