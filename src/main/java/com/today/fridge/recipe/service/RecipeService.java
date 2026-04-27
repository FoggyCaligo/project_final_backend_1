package com.today.fridge.recipe.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.intermediate.RecipeStepDTO;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeNutritionRepository recipeNutritionRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    public List<RecipeListResponse> getRecipes() {
        return List.of(
                RecipeListResponse.builder()
                        .recipeId(1L)
                        .title("토마토 파스타")
                        .summary("간단 파스타")
                        .cookTime("20분")
                        .build());
    }

    public List<RecipeListResponse> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        if (recipes.isEmpty()) {
            throw new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND);
        }
        return recipes.stream()
                .map(r -> RecipeListResponse.builder()
                        .recipeId(r.getRecipeId())
                        .title(r.getTitle())
                        .thumbnailUrl(r.getThumbnailUrl())
                        .summary(r.getSummary())
                        .cookTime(r.getCookTimeText())
                        .build())
                .collect(Collectors.toList());
    }

    public RecipeResponse getRecipe(Long recipeId) {

        // 레시피 정보 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND);
                });

        // 레시피 영양정보 조회
        RecipeNutrition nutrition = recipeNutritionRepository.findByRecipe_RecipeId(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피 영양정보를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NUTRITION_NOT_FOUND);
                });

        // 레시피 단계 조회
        List<RecipeStepDTO> recipeSteps = getRecipeAllSteps(recipeId);

        // 레시피 재료 조회
        List<RecipeIngredientDTO> recipeIngredients = getRecipeAllIngredients(recipeId);

        return RecipeResponse.of(recipe, nutrition, recipeSteps, recipeIngredients);
    }

    // ============================================================================================
    // 레시피의 단계 및 재료를 조회 및 반환하는 helper Method들입니다.
    // 일반 RecipeIngredient 및 RecipeStep Entity는 Recipe recipe를 포함하고 있다보니 이것을 사용하는 것이
    // 좋을 것 같았습니다.
    // ============================================================================================

    private List<RecipeStepDTO> getRecipeAllSteps(Long recipeId) {
        // 레시피 단계 조회
        List<RecipeStep> recipeSteps = recipeStepRepository.findByRecipe_RecipeId(recipeId);
        if (recipeSteps.isEmpty()) {
            log.error("레시피 단계 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllSteps에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_STEP_NOT_FOUND);
        }

        return recipeSteps.stream()
                .map(RecipeStepDTO::of)
                .collect(Collectors.toList());
    }

    private List<RecipeIngredientDTO> getRecipeAllIngredients(Long recipeId) {
        // 레시피 재료 조회
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipe_RecipeId(recipeId);
        if (recipeIngredients.isEmpty()) {
            log.error("레시피 재료 정보가 없습니다. recipeId: {}", recipeId);
            log.error("RecipeService.getRecipeAllIngredients에서 에러가 발생하였습니다.");
            throw new ExceptionTemplate(ErrorCode.RECIPE_INGREDIENT_NOT_FOUND);
        }

        return recipeIngredients.stream()
                .map(RecipeIngredientDTO::of)
                .collect(Collectors.toList());
    }
}
