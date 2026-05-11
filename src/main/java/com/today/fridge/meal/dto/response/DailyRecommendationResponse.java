package com.today.fridge.meal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecommendationResponse {
    private BigDecimal targetCalories;
    private BigDecimal targetCarbs;
    private BigDecimal targetProtein;
    private BigDecimal targetFat;
    private BigDecimal targetSugar;
    private BigDecimal targetSodium;
    private BigDecimal targetCholesterol;
    
    private BigDecimal currentCalories;
    private BigDecimal currentCarbs;
    private BigDecimal currentProtein;
    private BigDecimal currentFat;
    private BigDecimal currentSugar;
    private BigDecimal currentSodium;
    private BigDecimal currentCholesterol;
    
    private List<String> advice;
}
