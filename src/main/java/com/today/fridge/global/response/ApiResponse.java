package com.today.fridge.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data, requestId);
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data, String requestId) {
        return new ApiResponse<>(false, code, message, data, requestId);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }
}
