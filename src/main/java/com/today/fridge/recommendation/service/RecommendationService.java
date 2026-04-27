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
    	return List.of(
                RecipeRecommendationResponse.builder()
                        .recipeId(1L)
                        .title("토마토 파스타")
                        .matchRate(80.0)
                        .totalScore(86.0)
                        .matchedIngredients(List.of("토마토","양파"))
                        .missingIngredients(List.of("파스타면"))
                        .conditionTags(List.of("다이어트"))
                        .reason("보유 재료 일치율이 높고 사용자 조건에 적합한 레시피입니다.")
                        .build()
        );
    }
}