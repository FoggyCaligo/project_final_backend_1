package com.today.fridge.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FridgeIngredientItemResponse(
        Long ingredientId,
        Long ingredientMasterId,
        String rawName,
        String normalizedNameSnapshot,
        BigDecimal quantity,
        String unit,
        String storageType,
        LocalDate expirationDate,
        String freshnessStatus
) {
}
