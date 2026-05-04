package com.today.fridge.meal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemainingNutritionResponse {
    private BigDecimal remainingCalories;
    private BigDecimal remainingCarbs;
    private BigDecimal remainingProtein;
    private BigDecimal remainingFat;
    private BigDecimal remainingSugar;
    private BigDecimal remainingSodium;
    private BigDecimal remainingCholesterol;
}
