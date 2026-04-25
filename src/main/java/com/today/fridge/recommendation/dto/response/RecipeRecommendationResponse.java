package com.today.fridge.recommendation.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RecipeRecommendationResponse {

    private Long recipeId;
    private String title;
    private String thumbnailUrl;

    private double matchRate;
    private double totalScore;

    private List<String> matchedIngredients;
    private List<String> missingIngredients;
    private List<String> conditionTags;

    private String reason;
}