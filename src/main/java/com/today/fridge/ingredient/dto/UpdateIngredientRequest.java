package com.today.fridge.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 냉장고 CRUD API 작업 시작 문서 v1.0 §5 UpdateIngredientRequest.
 * PATCH 본문은 {@link com.today.fridge.ingredient.controller.FridgeIngredientController}에서
 * {@code Map<String, Object>}로 받아, 본 스키마와 동일한 키만 처리한다(전달된 필드만 변경).
 */
public record UpdateIngredientRequest(
        String name,
        BigDecimal quantity,
        String unit,
        String storageType,
        LocalDate expirationDate,
        Long categoryId
) {
}
