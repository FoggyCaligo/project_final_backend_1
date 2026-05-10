package com.today.fridge.substitution.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.llm.client.FastApiLlmClient;
import com.today.fridge.llm.dto.request.SubstitutionLlmRequest;
import com.today.fridge.llm.dto.response.SubstitutionLlmResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.substitution.dto.SubstitutionResultDto;
import com.today.fridge.substitution.dto.SubstitutionSuggestRequest;
import com.today.fridge.substitution.dto.SubstitutionSuggestResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubstitutionService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final FastApiLlmClient fastApiLlmClient;

    public SubstitutionSuggestResponse suggest(
            SubstitutionSuggestRequest request
    ) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() ->
                        new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND)
                );

        List<String> recipeIngredients =
                recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(
                        recipe.getRecipeId()
                );

        List<String> ownedIngredients =
                request.getOwnedIngredients() == null
                        ? List.of()
                        : request.getOwnedIngredients();

        List<String> missingIngredients =
                recipeIngredients.stream()
                        .filter(ingredient ->
                                !ownedIngredients.contains(ingredient)
                        )
                        .toList();

        SubstitutionLlmResponse llmResponse =
                fastApiLlmClient.suggestSubstitutions(
                        SubstitutionLlmRequest.builder()
                                .recipeTitle(recipe.getTitle())
                                .recipeIngredients(recipeIngredients)
                                .ownedIngredients(ownedIngredients)
                                .missingIngredients(missingIngredients)
                                .build()
                );

        List<SubstitutionResultDto> results =
                llmResponse.getResults().stream()
                        .map(result ->
                                SubstitutionResultDto.builder()
                                        .missingIngredient(result.getMissingIngredient())
                                        .decisionType(result.getDecisionType())
                                        .substituteIngredient(result.getSubstituteIngredient())
                                        .reason(result.getReason())
                                        .build()
                        )
                        .toList();

        return SubstitutionSuggestResponse.builder()
                .recipeId(recipe.getRecipeId())
                .recipeTitle(recipe.getTitle())
                .ownedIngredients(ownedIngredients)
                .missingIngredients(missingIngredients)
                .results(results)
                .build();
    }
}