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
public class ReportSummaryResponse {
    private BigDecimal averageCalories;
    private BigDecimal averageCarbs;
    private BigDecimal averageProtein;
    private BigDecimal averageFat;
    private BigDecimal averageSugar;
    private BigDecimal averageSodium;
    private BigDecimal averageCholesterol;
    private int missingDaysImputed;
    private List<ReportDailyData> dailyData;

    @Data
    @AllArgsConstructor
    public static class ReportDailyData {
        private String date; // ISO Date String e.g. "2023-10-01"
        private BigDecimal calories;
        private BigDecimal carbs;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal sugar;
        private BigDecimal sodium;
        private BigDecimal cholesterol;
        private boolean isImputed;
    }
}
