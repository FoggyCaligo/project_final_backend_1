package com.today.fridge.global.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getHttpStatus();
        this.code = errorCode.name();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.status = errorCode.getHttpStatus();
        this.code = errorCode.name();
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
