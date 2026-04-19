package com.todayfridge.backend1.global.error;

import java.util.List;

public class ErrorResponse {
    private final boolean success;
    private final String code;
    private final String message;
    private final List<FieldErrorItem> errors;
    private final String requestId;

    public ErrorResponse(boolean success, String code, String message, List<FieldErrorItem> errors, String requestId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.requestId = requestId;
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldErrorItem> getErrors() { return errors; }
    public String getRequestId() { return requestId; }

    public record FieldErrorItem(String field, String reason) { }
}
