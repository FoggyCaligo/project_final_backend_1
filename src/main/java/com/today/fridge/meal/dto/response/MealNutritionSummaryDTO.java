package com.today.fridge.meal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MealNutritionSummaryDTO {
    private BigDecimal totalCalories;
    private BigDecimal totalCarbs;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalSugar;
    private BigDecimal totalSodium;
    private BigDecimal totalCholesterol;

    public MealNutritionSummaryDTO(Number totalCalories, Number totalCarbs, Number totalProtein, Number totalFat, Number totalSugar, Number totalSodium, Number totalCholesterol) {
        this.totalCalories = totalCalories != null ? new BigDecimal(totalCalories.toString()) : BigDecimal.ZERO;
        this.totalCarbs = totalCarbs != null ? new BigDecimal(totalCarbs.toString()) : BigDecimal.ZERO;
        this.totalProtein = totalProtein != null ? new BigDecimal(totalProtein.toString()) : BigDecimal.ZERO;
        this.totalFat = totalFat != null ? new BigDecimal(totalFat.toString()) : BigDecimal.ZERO;
        this.totalSugar = totalSugar != null ? new BigDecimal(totalSugar.toString()) : BigDecimal.ZERO;
        this.totalSodium = totalSodium != null ? new BigDecimal(totalSodium.toString()) : BigDecimal.ZERO;
        this.totalCholesterol = totalCholesterol != null ? new BigDecimal(totalCholesterol.toString()) : BigDecimal.ZERO;
    }
}
