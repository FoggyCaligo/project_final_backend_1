package com.today.fridge.global.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseUUID<T> {

    private Meta meta;
    private T data;
    private Object error;

    public static <T> ApiResponseUUID<T> success(T data) {
        return ApiResponseUUID.<T>builder()
                .meta(Meta.builder()
                        .success(true)
                        .code("200")
                        .message("Success")
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .data(data)
                .build();
    }

    /**
     * code: 에러 코드
     * message: 에러 메세지
     * errorDetails: 에러 상세 내용
     */
    public static <T> ApiResponseUUID<T> error(String code, String message, Object errorDetails) {
        return ApiResponseUUID.<T>builder()
                .meta(Meta.builder()
                        .success(false)
                        .code(code)
                        .message(message)
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .error(errorDetails)
                .build();
    }

    /**
     * code: 에러 코드
     * message: 에러 메세지
     */
    public static <T> ApiResponseUUID<T> error(String code, String message) {
        return ApiResponseUUID.<T>builder()
                .meta(Meta.builder()
                        .success(false)
                        .code(code)
                        .message(message)
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .build();
    }

    /**
     * Meta 정보 클래스
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private boolean success;
        private String code;
        private String message;
        private String requestId;
    }
}
