package com.today.fridge.meal.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MealLogRequest {
    private Long userId;
    private Long recipeId;
    private BigDecimal servings;
    private LocalDateTime consumedAt;
}