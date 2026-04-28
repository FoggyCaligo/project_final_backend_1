package com.today.fridge.global.response;

/** 냉장고 CRUD API 작업 시작 문서 v1.0 §4.2 pageInfo 객체와 동일 필드. */
public record PageResponse(long totalElements, int totalPages, int page, int size) {
}
