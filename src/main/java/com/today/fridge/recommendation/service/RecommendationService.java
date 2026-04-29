package com.today.fridge.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.embedding.service.RecipeEmbeddingSearchService;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.llm.service.RecommendationExplanationService;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.ConditionWarningDto;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.substitution.service.SubstituteIngredientService;
import com.today.fridge.llm.dto.request.RecommendationExplanationContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final UserConditionRepository userConditionRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;
    private final RecommendationScoreService recommendationScoreService;
    private final SubstituteIngredientService substituteIngredientService;
    private final RecommendationReasonService recommendationReasonService;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final AllergyFilterService allergyFilterService;
    private final RecipeEmbeddingSearchService recipeEmbeddingSearchService;
    private final HybridRankingService hybridRankingService;
    private final RecommendationExplanationService recommendationExplanationService;
    
    private RecipeRecommendationResponse createRecipeResponse(
            Recipe recipe,
            List<String> requiredIngredients,
            double conditionScore,
            List<String> conditionTags,
            List<String> ownedIngredients,
            List<ConditionWarningDto> warnings,
            double semanticScore,
            boolean useLlmExplanation
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
        
        double hybridScore =
                hybridRankingService.calculateHybridScore(
                        totalScore,
                        semanticScore
                );
        
        String reason = recommendationReasonService.buildReason(
                Math.round(matchRate * 10) / 10.0,
                conditionTags,
                missingIngredients
        );
        String llmExplanation = useLlmExplanation
                ? recommendationExplanationService.generateExplanation(
                        new RecommendationExplanationContext(
                                recipe.getRecipeId(),
                                recipe.getTitle(),
                                matchedIngredients,
                                missingIngredients,
                                conditionTags,
                                Math.round(matchRate * 10) / 10.0,
                                Math.round(totalScore * 10) / 10.0,
                                Math.round(semanticScore * 1000) / 1000.0,
                                Math.round(hybridScore * 10) / 10.0,
                                reason
                        )
                )
                : null;
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
                .warnings(warnings)
                .substituteSuggestions(
                        substituteIngredientService.suggest(
                                missingIngredients,
                                ownedIngredients,
                                recipe.getTitle()
                        )
                )
                .reason(reason)
                .llmExplanation(llmExplanation)
                .semanticScore(
                	    Math.round(semanticScore * 1000) / 1000.0
                	)
                	.hybridScore(
                	    Math.round(hybridScore * 10) / 10.0
                	)
                	
                .build();
    }
    private List<ConditionWarningDto> buildWarnings(
            List<UserCondition> userConditions,
            List<RecipeConditionMap> recipeConditions
    ) {
        List<Long> userConditionIds = userConditions.stream()
                .map(uc -> uc.getConditionCode().getConditionId())
                .toList();

        return recipeConditions.stream()
                .filter(rc -> userConditionIds.contains(
                        rc.getConditionCode().getConditionId()
                ))
                .filter(rc -> "CAUTION".equals(rc.getFitType()))
                .map(rc -> ConditionWarningDto.builder()
                        .conditionCode(rc.getConditionCode().getConditionCode())
                        .conditionName(rc.getConditionCode().getConditionName())
                        .warningMessage(
                                rc.getConditionCode().getConditionName()
                                        + " 조건에 주의가 필요한 레시피입니다."
                        )
                        .build())
                .toList();
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
    	        query.isUseUserFridge() && query.getUserId() != null
    	                ? userIngredientRepository.findOwnedIngredientNamesByUserId(query.getUserId())
    	                : query.getIncludeIngredients() == null
    	                        ? List.of()
    	                        : query.getIncludeIngredients();

        List<Recipe> recipes = recipeRepository.findByIsActiveTrue();

        List<UserCondition> userConditions =
                query.isUseUserProfile() && query.getUserId() != null
                        ? userConditionRepository.findByUser_UserIdAndIsActiveTrue(query.getUserId())
                        : List.of();
        // 사용자 알러지 코드 추출 (condition_code reuse 중이면)
        List<String> userAllergenCodes =
                userConditions.stream()
                        .map(uc -> uc.getConditionCode().getConditionCode())
                        .filter(code -> code.startsWith("ALLERGY"))
                        .map(code -> code.replace("ALLERGY_", ""))
                        .toList();


        // hard filter
        if (!userAllergenCodes.isEmpty()) {
            recipes = recipes.stream()
                    .filter(recipe -> {
                        List<String> recipeIngredients =
                                recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(
                                        recipe.getRecipeId()
                                );

                        return !allergyFilterService.containsAllergen(
                                recipeIngredients,
                                userAllergenCodes
                        );
                    })
                    .toList();
        }
        boolean useHybridRanking =
                "CHATBOT".equalsIgnoreCase(query.getSource());

        final Map<Long, Double> semanticScoreMap;

        if (useHybridRanking) {

            String semanticQuery =
                    String.join(" ", query.getKeywords());

            if (semanticQuery.isBlank()) {
                semanticQuery = String.join(" ", ownedIngredients);
            }

            var semanticResults =
                    recipeEmbeddingSearchService.searchSimilarRecipes(
                            semanticQuery,
                            50
                    );

            semanticScoreMap =
                    semanticResults.stream()
                            .collect(Collectors.toMap(
                                    r -> r.getRecipeId(),
                                    r -> hybridRankingService.toSemanticScore(
                                            r.getDistance()
                                    )
                            ));
        } else {
            semanticScoreMap = Map.of();
        }
        return recipes.stream()
                .map(recipe -> {
                    List<String> requiredIngredients =
                            recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(
                                    recipe.getRecipeId()
                            );

                    List<RecipeConditionMap> recipeConditions =
                            recipeConditionMapRepository.findByRecipe_RecipeId(
                                    recipe.getRecipeId()
                            );

                    double conditionScore =
                            recommendationScoreService.calculateConditionScore(
                                    userConditions,
                                    recipeConditions
                            );

                    List<String> conditionTags = userConditions.stream()
                            .map(uc -> uc.getConditionCode().getConditionName())
                            .distinct()
                            .toList();
                    List<ConditionWarningDto> warnings = buildWarnings(
                            userConditions,
                            recipeConditions
                    );
                    double semanticScore =
                            semanticScoreMap.getOrDefault(
                                    recipe.getRecipeId(),
                                    0.0
                            );
                    return createRecipeResponse(
                            recipe,
                            requiredIngredients,
                            conditionScore,
                            conditionTags,
                            ownedIngredients,
                            warnings,
                            semanticScore,
                            useHybridRanking
                    );
                })
                .sorted((a, b) -> {
                    if (useHybridRanking) {
                        return Double.compare(b.getHybridScore(), a.getHybridScore());
                    }
                    return Double.compare(b.getTotalScore(), a.getTotalScore());
                })
                .toList();
    }
}