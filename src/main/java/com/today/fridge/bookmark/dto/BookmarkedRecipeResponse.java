package com.today.fridge.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkedRecipeResponse {
    private Long recipeId;
    private String title;
    private String thumbnailUrl;
    private String summary;
    private String servingsText;
    private String cookTimeText;
    private String difficultyLevel;
}
