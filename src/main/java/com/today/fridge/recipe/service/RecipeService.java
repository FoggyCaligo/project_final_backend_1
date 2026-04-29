package com.today.fridge.recipe.service;

/*
 * RecipeService는 현재 3개의 Method를 제공하고 있습니다.
 * getRecipes: 목레시피를 반환합니다. (목레시피 데이터를 반환합니다.)
 * getAllRecipes: 모든 레시피를 반환합니다. (페이징을 고려하지 않았습니다.)
 * getRecipe: 특정 레시피를 반환합니다.
 * 
 * Recipe/DTO/response/RecipeResponse를 사용하여 1개의 레시피 또는 모든 레세피를 제공합니다.
 * 
 * RecipeStepDTO 및 RecipeIngredientDTO는 레시피의 재료 및 단계 정보를 제공하기 위한 DTO입니다.
 * 이것이 없으면 N+1이 발생할 가능성이 있다 판단하여 작성하였습니다.
 * 이것들은 밑에 존재하는 getRecipeAllSteps와 getRecipeAllIngredients에서 사용됩니다.
 */
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// Response DTO
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;

import lombok.RequiredArgsConstructor;

//Intermediate DTO -> Step 및 Ingredient 조회
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.intermediate.RecipeStepDTO;

// 각 table의 Entity
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;

// 각 table의 Repository
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;

// Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Global Exception
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

    public RecipeResponse getRecipe(Long recipeId) {

        // 레시피 정보 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
                });

        // 레시피 영양정보 조회
        RecipeNutrition nutrition = recipeNutritionRepository.findByRecipe_RecipeId(recipeId)
                .orElseThrow(() -> {
                    log.error("레시피 영양정보를 찾을 수 없습니다. recipeId: {}", recipeId);
                    log.error("RecipeService.getRecipe에서 에러가 발생하였습니다.");
                    return new ExceptionTemplate(ErrorCode.RECIPE_NUTRITION_NOT_FOUND,
                            java.util.Map.of("recipeId", recipeId));
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
            throw new ExceptionTemplate(ErrorCode.RECIPE_STEP_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
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
            throw new ExceptionTemplate(ErrorCode.RECIPE_INGREDIENT_NOT_FOUND, java.util.Map.of("recipeId", recipeId));
        }

        return recipeIngredients.stream()
                .map(RecipeIngredientDTO::of)
                .collect(Collectors.toList());
    }

    public PageResult<RecipeListResponse> getRecipes(Pageable pageable) {

        Page<Recipe> recipePage =
                recipeRepository.findByIsActiveTrue(pageable);

        List<RecipeListResponse> content =
                recipePage.getContent()
                        .stream()
                        .map(RecipeListResponse::from)
                        .toList();

        PageResponse pageInfo = new PageResponse(
                recipePage.getTotalElements(),
                recipePage.getTotalPages(),
                recipePage.getNumber(),
                recipePage.getSize()
        );

        return new PageResult<>(content, pageInfo);
    }
}
