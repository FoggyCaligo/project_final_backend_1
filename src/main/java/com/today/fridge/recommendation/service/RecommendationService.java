package com.today.fridge.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserConditionRepository userConditionRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;
    private final RecommendationScoreService recommendationScoreService;

    private RecipeRecommendationResponse createMockRecipe(
            Long recipeId,
            String title,
            int matchedCount,
            int requiredCount,
            double conditionScore,
            List<String> conditionTags,
            List<String> missingIngredients
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
                .build();
    }
    
    public List<RecipeRecommendationResponse> recommend(Long userId) {

    	// TODO : DB seed 이후실제 추천로직 활성화
    	/*
        // 사용자 조건 조회
        List<UserCondition> userConditions =
                userConditionRepository
                        .findByUser_UserIdAndIsActiveTrue(userId);

        // 임시 recipeId
        Long recipeId = 1L;

        List<RecipeConditionMap> recipeConditions =
                recipeConditionMapRepository
                        .findByRecipe_RecipeId(recipeId);

        double conditionScore =
                recommendationScoreService.calculateConditionScore(
                        userConditions,
                        recipeConditions
                );

        return List.of(
                RecipeRecommendationResponse.builder()
                        .recipeId(recipeId)
                        .title("토마토 파스타")
                        .matchRate(80.0)
                        .totalScore(conditionScore + 56.0)
                        .conditionTags(
                                recipeConditions.stream()
                                        .map(rc -> rc.getConditionCode().getConditionName())
                                        .toList()
                        )
                        .reason("사용자 조건 기반 추천 결과입니다.")
                        .build()
        );
        */
    	
    	// 임시 mock
    	List<RecipeRecommendationResponse> mockRecipes = List.of(
    	        createMockRecipe(1L, "토마토 파스타", 4, 5, 20.0,
    	                List.of("다이어트"), List.of("파스타면")),
    	        createMockRecipe(2L, "두부 샐러드", 5, 5, 30.0,
    	                List.of("다이어트", "저당"), List.of()),
    	        createMockRecipe(3L, "된장찌개", 3, 6, 10.0,
    	                List.of("저염"), List.of("두부", "애호박"))
    	);

    	return mockRecipes.stream()
    	        .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
    	        .toList();
    }
}