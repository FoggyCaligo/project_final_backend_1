package com.today.fridge.global.exception;

import lombok.Getter;

@Getter
public class ExceptionTemplate extends RuntimeException {
    private final ErrorCode errorCode;

    public ExceptionTemplate(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}