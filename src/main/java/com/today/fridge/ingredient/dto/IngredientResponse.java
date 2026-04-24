package com.today.fridge.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 지침서 v1.0 IngredientResponse */
public record IngredientResponse(
        Long ingredientId,
        String name,
        String normalizedName,
        String category,
        LocalDate expirationDate,
        BigDecimal quantity,
        String unit,
        String storageType,
        String freshnessStatus
) {
}
