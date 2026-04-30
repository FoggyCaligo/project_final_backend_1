package com.today.fridge.llm.dto.request;

import java.util.List;

// FastApi 내부호출용
public record RecommendationExplanationContext(
        Long recipeId,
        String title,
        List<String> matchedIngredients,
        List<String> missingIngredients,
        List<String> conditionTags,
        Double matchRate,
        Double totalScore,
        Double semanticScore,
        Double hybridScore,
        String reason
) {
}