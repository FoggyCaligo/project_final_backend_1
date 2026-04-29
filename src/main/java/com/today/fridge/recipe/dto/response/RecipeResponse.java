package com.today.fridge.recipe.dto.response;

/*
 * 레시피 상세 정보 DTO
 * of Method를 사용하여 레시피 정보와 영양 정보를 받아서 
 * builder pattern을 사용하여 immutable한 정보를 생성하도록 함
 */

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.intermediate.RecipeStepDTO;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeResponse {
    // Recipe 이내 자료
    private Long recipeId;
    private String sourceSite;
    private String sourceRecipeKey;
    private String title;
    private String thumbnailUrl;
    private String summary;
    private String servingsText;
    private String cookTimeText;
    private String sourceUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Recipe Nutrition.java
    private BigDecimal referenceWeight;
    private BigDecimal calories;
    private BigDecimal carbs;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal sugar;
    private BigDecimal sodium;
    private BigDecimal cholesterol;

    private List<RecipeStepDTO> recipeSteps;
    private List<RecipeIngredientDTO> recipeIngredients;

    /*
     * Recipe의 정보 및 영양 정보를 받아 RecipeResponse를 생성
     */
    public static RecipeResponse of(Recipe recipe, RecipeNutrition nutrition, List<RecipeStepDTO> recipeSteps,
            List<RecipeIngredientDTO> recipeIngredients) {
        RecipeResponseBuilder builder = RecipeResponse.builder()

                // 레시피 정보
                .recipeId(recipe.getRecipeId())
                .sourceSite(recipe.getSourceSite())
                .sourceRecipeKey(recipe.getSourceRecipeKey())
                .title(recipe.getTitle())
                .thumbnailUrl(recipe.getThumbnailUrl())
                .summary(recipe.getSummary())
                .servingsText(recipe.getServingsText())
                .cookTimeText(recipe.getCookTimeText())
                .sourceUrl(recipe.getSourceUrl())
                .isActive(recipe.getIsActive())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt());

        // 2. 정보가 있을 경우 영양정보를 추가, 없으면 제거
        if (nutrition != null) {
            builder.referenceWeight(nutrition.getReferenceWeight())
                    .calories(nutrition.getCalories())
                    .carbs(nutrition.getCarbs())
                    .protein(nutrition.getProtein())
                    .fat(nutrition.getFat())
                    .sugar(nutrition.getSugar())
                    .sodium(nutrition.getSodium())
                    .cholesterol(nutrition.getCholesterol());
        } else {
            builder.referenceWeight(BigDecimal.ZERO)
                    .calories(BigDecimal.ZERO)
                    .carbs(BigDecimal.ZERO)
                    .protein(BigDecimal.ZERO)
                    .fat(BigDecimal.ZERO)
                    .sugar(BigDecimal.ZERO)
                    .sodium(BigDecimal.ZERO)
                    .cholesterol(BigDecimal.ZERO);
        }
        // 레시피 단계 및 재료 정보 추가
        builder.recipeSteps(recipeSteps)
                .recipeIngredients(recipeIngredients);

        return builder.build();
    }
}
