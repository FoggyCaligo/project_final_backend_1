package com.today.fridge.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.substitution.service.SubstituteIngredientService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserConditionRepository userConditionRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;
    private final RecommendationScoreService recommendationScoreService;
    private final SubstituteIngredientService substituteIngredientService;

    private RecipeRecommendationResponse createMockRecipe(
            Long recipeId,
            String title,
            int matchedCount,
            int requiredCount,
            double conditionScore,
            List<String> conditionTags,
            List<String> missingIngredients,
            List<String> ownedIngredients
    ) {
        double ingredientScore =
                recommendationScoreService.calculateIngredientScore(matchedCount, requiredCount);

        double matchRate =
                recommendationScoreService.calculateMatchRate(matchedCount, requiredCount);

        double totalScore =
                recommendationScoreService.calculateTotalScore(ingredientScore, conditionScore);

        return RecipeRecommendationResponse.builder()
                .recipeId(recipeId)
                .title(title)
                .matchRate(Math.round(matchRate * 10) / 10.0)
                .totalScore(Math.round(totalScore * 10) / 10.0)
                .matchedIngredients(List.of("보유 재료"))
                .missingIngredients(missingIngredients)
                .conditionTags(conditionTags)
                .reason("보유 재료와 사용자 조건을 기준으로 추천된 레시피입니다.")
                .substituteSuggestions(
                	    substituteIngredientService.suggest(
                	        missingIngredients,
                	        ownedIngredients,
                	        title
                	    )
                	)
                .build();
    }
    
    public List<RecipeRecommendationResponse> recommend(Long userId) {
        return recommend(
                RecommendationQuery.builder()
                        .userId(userId)
                        .conditionCodes(List.of())
                        .includeIngredients(List.of())
                        .excludeIngredients(List.of())
                        .keywords(List.of())
                        .sortHint("ingredient_match")
                        .source("HOME")
                        .useUserProfile(true)
                        .useUserFridge(true)
                        .build()
        );
    }
    
    public List<RecipeRecommendationResponse> recommend(RecommendationQuery query) {
    	List<String> ownedIngredients =
    	        query.getIncludeIngredients() == null
    	                ? List.of()
    	                : query.getIncludeIngredients();
    	
    	List<String> conditionCodes =
    	        query.getConditionCodes() == null
    	                ? List.of()
    	                : query.getConditionCodes();
    	
        List<RecipeRecommendationResponse> mockRecipes = List.of(
        		createMockRecipe(1L, "토마토 파스타", 2, 5, 20.0,
        				conditionCodes, List.of("파스타면"), ownedIngredients),

        		createMockRecipe(2L, "두부 샐러드", 2, 2, 30.0,
        				conditionCodes, List.of(), ownedIngredients),

        		createMockRecipe(3L, "양배추 두부볶음", 2, 3, 25.0,
        		        conditionCodes, List.of("간장"), ownedIngredients)
        );

        return mockRecipes.stream()
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .toList();
    }
}