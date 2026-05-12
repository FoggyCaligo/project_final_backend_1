package com.today.fridge.meal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FastApiHealthReportRequest {
    private List<String> fridge_ingredients;
    private List<Map<String, Object>> recent_meals;
}
