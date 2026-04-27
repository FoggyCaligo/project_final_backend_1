package com.today.fridge.recipe.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeListResponse {
    private Long recipeId;
    private String title;
    private String thumbnailUrl;
    private String summary;
    private String cookTime;
}