package com.today.fridge.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
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
    private final RecommendationReasonService recommendationReasonService;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    
    private RecipeRecommendationResponse createRecipeResponse(
            Recipe recipe,
            List<String> requiredIngredients,
            double conditionScore,
            List<String> conditionTags,
            List<String> ownedIngredients
    ) {
        List<String> matchedIngredients = requiredIngredients.stream()
                .filter(ownedIngredients::contains)
                .toList();

        List<String> missingIngredients = requiredIngredients.stream()
                .filter(ingredient -> !ownedIngredients.contains(ingredient))
                .toList();

        int matchedCount = matchedIngredients.size();
        int requiredCount = requiredIngredients.size();

        double ingredientScore =
                recommendationScoreService.calculateIngredientScore(matchedCount, requiredCount);

        double matchRate =
                recommendationScoreService.calculateMatchRate(matchedCount, requiredCount);

        double totalScore =
                recommendationScoreService.calculateTotalScore(ingredientScore, conditionScore);

        String reason = recommendationReasonService.buildReason(
                Math.round(matchRate * 10) / 10.0,
                conditionTags,
                missingIngredients
        );

        return RecipeRecommendationResponse.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .summary(recipe.getSummary())
                .cookTimeText(recipe.getCookTimeText())
                .thumbnailUrl(recipe.getThumbnailUrl())
                .matchRate(Math.round(matchRate * 10) / 10.0)
                .totalScore(Math.round(totalScore * 10) / 10.0)
                .matchedIngredients(matchedIngredients)
                .missingIngredients(missingIngredients)
                .conditionTags(conditionTags)
                .substituteSuggestions(
                        substituteIngredientService.suggest(
                                missingIngredients,
                                ownedIngredients,
                                recipe.getTitle()
                        )
                )
                .reason(reason)
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

        List<Recipe> recipes = recipeRepository.findByIsActiveTrue();

        return recipes.stream()
                .map(recipe -> {
                    List<String> requiredIngredients =
                            recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(
                                    recipe.getRecipeId()
                            );

                    return createRecipeResponse(
                            recipe,
                            requiredIngredients,
                            20.0, // TODO: condition map 기반 점수로 교체
                            conditionCodes,
                            ownedIngredients
                    );
                })
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .toList();
    }
}