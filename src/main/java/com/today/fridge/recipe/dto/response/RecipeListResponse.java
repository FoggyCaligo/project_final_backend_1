package com.today.fridge.recipe.dto.response;

import com.today.fridge.recipe.entity.Recipe;
import lombok.Builder;

@Builder
public record RecipeListResponse(
        Long recipeId,
        String title,
        String thumbnailUrl,
        String summary,
        String servingsText,
        String cookTimeText
) {
    public static RecipeListResponse from(Recipe recipe) {
        return RecipeListResponse.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .thumbnailUrl(recipe.getThumbnailUrl())
                .summary(recipe.getSummary())
                .servingsText(recipe.getServingsText())
                .cookTimeText(recipe.getCookTimeText())
                .build();
    }
}