package com.today.fridge.substitution.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
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
                request.getOwnedIngredients();

        List<String> missingIngredients =
                recipeIngredients.stream()
                        .filter(ingredient ->
                                !ownedIngredients.contains(ingredient)
                        )
                        .toList();

        // TODO:
        // 이후 여기서 FastAPI LLM 호출 예정

        List<SubstitutionResultDto> results =
                List.of();

        return SubstitutionSuggestResponse.builder()
                .recipeId(recipe.getRecipeId())
                .recipeTitle(recipe.getTitle())
                .ownedIngredients(ownedIngredients)
                .missingIngredients(missingIngredients)
                .results(results)
                .build();
    }
}