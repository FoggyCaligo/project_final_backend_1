package com.today.fridge.recipe.dto.intermediate;

import com.today.fridge.recipe.entity.RecipeIngredient;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeIngredientDTO {
    private String rawText;
    private String normalizedNameSnapshot;
    private String amountText;
    private String unit;
    private Boolean isOptional;
    private Integer sortOrder;

    public static RecipeIngredientDTO of(RecipeIngredient ingredient) {
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
