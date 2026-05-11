package com.today.fridge.recommendation.dto.response;

import java.util.List;

import com.today.fridge.substitution.dto.SubstituteSuggestionDto;

import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class RecipeRecommendationResponse {

    private Long recipeId;
    private String title;
    private String thumbnailUrl;
    private String cookTimeText;
    private String summary;

    private double matchRate;

    // rule 기반 점수
    private double totalScore;

    // semantic 기반 점수
    private double semanticScore;

    // 최종 병합 점수
    private double hybridScore;
    
    private double tagScore;

    private List<String> matchedIngredients;
    private List<String> missingIngredients;
    private List<String> conditionTags;
    private List<SubstituteSuggestionDto> substituteSuggestions;
    
    private List<String> ownedIngredients;

    private List<ConditionWarningDto> warnings;

    private String reason;
	private String llmExplanation;
}