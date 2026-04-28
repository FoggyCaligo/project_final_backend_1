package com.today.fridge.recommendation.dto.response;

import java.util.List;

import com.today.fridge.substitution.dto.SubstituteSuggestionDto;

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
    private List<SubstituteSuggestionDto> substituteSuggestions;

    private String reason;
}