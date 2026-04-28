package com.today.fridge.ingredient.dto;

/** GET /api/v1/fridge/categories 응답 항목 */
public record CategoryResponse(
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer sortOrder
) {
}
