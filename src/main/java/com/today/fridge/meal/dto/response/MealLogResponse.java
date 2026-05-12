package com.today.fridge.meal.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MealLogResponse {

    private Long mealId;
    private Long recipeId;
    private String recipeTitle;
    private BigDecimal servings;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;

    public MealLogResponse(Long mealId, Long recipeId, String recipeTitle, BigDecimal servings,
            LocalDateTime consumedAt, LocalDateTime createdAt) {
        this.mealId = mealId;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.servings = servings;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }
}
