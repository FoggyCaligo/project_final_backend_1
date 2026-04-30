package com.today.fridge.recommendation.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.entity.UserCondition;

@Component
public class RecommendationScoreService {

    private static final double MAX_INGREDIENT_SCORE = 70.0;
    private static final double CONDITION_MATCH_SCORE = 10.0;
    private static final double MAX_CONDITION_SCORE = 30.0;
    private static final double CAUTION_PENALTY = -30.0;
    private static final double MISSING_PENALTY = 1.0;
    private static final double MANY_MISSING_EXTRA_PENALTY = 5.0;

    public double calculateIngredientScore(int matchedCount, int requiredCount) {
        if (requiredCount <= 0) {
            return 0.0;
        }

        double matchRate = (double) matchedCount / requiredCount;
        int missingCount = requiredCount - matchedCount;

        double penalty = missingCount * MISSING_PENALTY;

        if (missingCount >= 5) {
            penalty += MANY_MISSING_EXTRA_PENALTY;
        }

        double score = (matchRate * MAX_INGREDIENT_SCORE) - penalty;
        
        if (matchedCount > 0) {
            score += (matchedCount * 5);
        }
        
        return score < 0 ? 3.0 : score;
    }

    public double calculateMatchRate(int matchedCount, int requiredCount) {
        if (requiredCount <= 0) {
            return 0.0;
        }
        

        return ((double) matchedCount / requiredCount) * 100;
    }

    public double calculateConditionScore(
            List<UserCondition> userConditions,
            List<RecipeConditionMap> recipeConditions
    ) {
        if (userConditions == null || userConditions.isEmpty()) {
            return 0.0;
        }

        if (recipeConditions == null || recipeConditions.isEmpty()) {
            return 0.0;
        }

        Set<Long> userConditionIds = userConditions.stream()
                .map(uc -> uc.getConditionCode().getConditionId())
                .collect(Collectors.toSet());

        boolean hasCaution = recipeConditions.stream()
                .anyMatch(rc ->
                        userConditionIds.contains(rc.getConditionCode().getConditionId())
                                && "CAUTION".equalsIgnoreCase(rc.getFitType())
                );

        if (hasCaution) {
            return CAUTION_PENALTY;
        }

        long matchedCount = recipeConditions.stream()
                .filter(rc -> userConditionIds.contains(rc.getConditionCode().getConditionId()))
                .filter(rc -> !"CAUTION".equalsIgnoreCase(rc.getFitType()))
                .count();

        return Math.min(matchedCount * CONDITION_MATCH_SCORE, MAX_CONDITION_SCORE);
    }

    public double calculateTotalScore(double ingredientScore, double conditionScore) {
        double totalScore = (ingredientScore * 0.7) + (conditionScore * 0.3);

        if (conditionScore > 0) {
            totalScore += 5.0;
        }

        return totalScore;
    }
    public double calculateConditionScoreByCodes(
            List<String> conditionCodes,
            List<RecipeConditionMap> recipeConditions
    ) {
        if (conditionCodes == null || conditionCodes.isEmpty()
                || recipeConditions == null || recipeConditions.isEmpty()) {
            return 0.0;
        }

        boolean hasCaution = recipeConditions.stream()
                .anyMatch(rc ->
                        conditionCodes.contains(rc.getConditionCode().getConditionCode())
                                && "CAUTION".equalsIgnoreCase(rc.getFitType())
                );

        if (hasCaution) {
            return CAUTION_PENALTY;
        }

        long matchedCount = recipeConditions.stream()
                .filter(rc -> conditionCodes.contains(rc.getConditionCode().getConditionCode()))
                .filter(rc -> !"CAUTION".equalsIgnoreCase(rc.getFitType()))
                .count();

        return Math.min(matchedCount * CONDITION_MATCH_SCORE, MAX_CONDITION_SCORE);
    }
}