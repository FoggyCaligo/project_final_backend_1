package com.today.fridge.global.exception;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ExceptionTemplate extends RuntimeException {
    private final ErrorCode errorCode;
}