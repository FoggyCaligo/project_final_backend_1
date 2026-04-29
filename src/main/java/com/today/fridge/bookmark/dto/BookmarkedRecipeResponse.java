package com.today.fridge.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BookmarkedRecipeResponse {
    private Long recipeId;
    private String recipeName; // DB의 title 값이 여기에 담깁니다.
}