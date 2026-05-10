package com.today.fridge.global.external.fastapi;

import java.util.List;
import java.util.Map;

public record MealRecommendationResponse(
    String report,
    Map<String, List<Integer>> recommendations
) {
}
