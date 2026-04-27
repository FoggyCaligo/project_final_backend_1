package com.today.fridge.recommendation.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.entity.UserCondition;

@Component
public class RecommendationScoreService {

	private static final double CONDITION_MATCH_SCORE = 10.0;
	private static final double MAX_CONDITION_SCORE = 30.0;
	private static final double CAUTION_PENALTY = -30.0;
	
	public double calculateConditionScore(List<UserCondition> userConditions, List<RecipeConditionMap> recipeConditions) {
		
		if(userConditions == null || userConditions.isEmpty()) {
			return 0.0;
		}
		if (recipeConditions == null || recipeConditions.isEmpty()) {
			return 0.0;
		}
		
		Set<Long> userConditionIds = userConditions.stream()
		        .map(uc -> uc.getConditionCode().getConditionId())
		        .collect(Collectors.toSet());
		
		boolean hasCaution = recipeConditions.stream()
                .anyMatch(recipeCondition ->
                        userConditionIds.contains(recipeCondition.getConditionCode().getConditionId())
                                && "CAUTION".equalsIgnoreCase(recipeCondition.getFitType())
                );
		
		if (hasCaution) {
			return CAUTION_PENALTY;
		}
		
		long matchedCount = recipeConditions.stream()
		        .filter(recipeCondition ->
		                userConditionIds.contains(recipeCondition.getConditionCode().getConditionId())
		        )
		        .filter(recipeCondition ->
		                !"CAUTION".equalsIgnoreCase(recipeCondition.getFitType())
		        )
		        .count();
		
		return Math.min(matchedCount * CONDITION_MATCH_SCORE, MAX_CONDITION_SCORE);
		}
}
