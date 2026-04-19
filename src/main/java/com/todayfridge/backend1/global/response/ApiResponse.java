package com.todayfridge.backend1.global.response;

import org.slf4j.MDC;

public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final String requestId;

    public ApiResponse(boolean success, String code, String message, T data, String requestId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data, MDC.get("requestId"));
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, MDC.get("requestId"));
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getRequestId() { return requestId; }
}
