package com.today.fridge.shopping.exception;

import com.today.fridge.global.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShoppingErrorCode {

    // 502 Bad Gateway - 쇼핑 외부 API
    NAVER_API_FAILED(HttpStatus.BAD_GATEWAY, "네이버 쇼핑 API 호출에 실패했습니다."),
    COUPANG_API_FAILED(HttpStatus.BAD_GATEWAY, "쿠팡 파트너스 API 호출에 실패했습니다."),
    ALL_SHOPPING_API_FAILED(HttpStatus.BAD_GATEWAY, "모든 쇼핑 API 호출에 실패했습니다."),

    // 404 Not Found
    INGREDIENT_MASTER_NOT_FOUND(HttpStatus.NOT_FOUND, "식재료 마스터 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public BusinessException toException() {
        return new BusinessException(httpStatus, this.name(), message);
    }

    public BusinessException toException(String detail) {
        return new BusinessException(httpStatus, this.name(), detail);
    }
}
