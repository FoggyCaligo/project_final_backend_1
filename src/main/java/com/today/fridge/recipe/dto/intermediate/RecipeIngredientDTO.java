package com.today.fridge.recipe.dto.intermediate;

/*
 * Recipe Ingredient의 경우, Recipe recipe가 존재하다 보니 N+1 발생할 수 있는 가능성이 존재함.
 * 이를 방지하기 위해 RecipeIngredientDTO를 사용함. 
 * 이 DTO는 Recipe/Service의 getRecipeAllIngredients에서 사용됩니다.
 */
import com.today.fridge.recipe.entity.RecipeIngredient;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredientDTO {
    private String rawText;
    private String normalizedNameSnapshot;
    private String amountText;
    private String unit;
    private Boolean isOptional;
    private Integer sortOrder;

    public static RecipeIngredientDTO of(RecipeIngredient ingredient) {
        if (ingredient == null)
            return null;
        return RecipeIngredientDTO.builder()
                .rawText(ingredient.getRawText())
                .normalizedNameSnapshot(ingredient.getNormalizedNameSnapshot())
                .amountText(ingredient.getAmountText())
                .unit(ingredient.getUnit())
                .isOptional(ingredient.getIsOptional())
                .sortOrder(ingredient.getSortOrder())
                .build();
    }
}
