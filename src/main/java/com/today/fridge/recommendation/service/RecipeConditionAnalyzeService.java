package com.today.fridge.recommendation.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeConditionAnalyzeService {

    private final RecipeRepository recipeRepository;
    private final ConditionCodeRepository conditionCodeRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;

    @Transactional
    public void saveDummyAnalysis(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow();

        List<ConditionCode> conditions =
                conditionCodeRepository.findByIsActiveTrue();

        for (ConditionCode condition : conditions) {
            String fitType = "ALLOWED";
            String sourceType = "LLM";
            BigDecimal confidenceScore = BigDecimal.valueOf(0.80);

            RecipeConditionMap map = recipeConditionMapRepository
                    .findByRecipe_RecipeIdAndConditionCode_ConditionId(
                            recipeId,
                            condition.getConditionId()
                    )
                    .map(existing -> {
                        existing.updateAnalysis(
                                fitType,
                                sourceType,
                                confidenceScore
                        );
                        return existing;
                    })
                    .orElseGet(() ->
                            RecipeConditionMap.create(
                                    recipe,
                                    condition,
                                    fitType,
                                    sourceType,
                                    confidenceScore
                            )
                    );

            recipeConditionMapRepository.save(map);
        }
    }
}