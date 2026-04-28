package com.today.fridge.global.exception;

import lombok.Getter;

@Getter
public class ExceptionTemplate extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object payload;

    public ExceptionTemplate(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.payload = null;
    }

    public ExceptionTemplate(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.payload = null;
    }

    public ExceptionTemplate(ErrorCode errorCode, Object payload) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.payload = payload;
    }

    public ExceptionTemplate(ErrorCode errorCode, String customMessage, Object payload) {
        super(customMessage);
        this.errorCode = errorCode;
        this.payload = payload;
    }
}