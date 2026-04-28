package com.today.fridge.ingredient.dto;

import java.time.LocalDate;

public record SoonItemResponse(
        Long ingredientId,
        String name,
        LocalDate expirationDate,
        String freshnessStatus
) {
}
