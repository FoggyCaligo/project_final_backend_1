package com.today.fridge.recommendation.dto.response;

public record RecipeRecommendationRow(
        Long recipeId,
        String title,
        String thumbnailUrl,
        String summary,
        String cookTimeText,
        String difficultyLevel
) {
}