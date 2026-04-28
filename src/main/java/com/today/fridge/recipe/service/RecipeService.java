package com.today.fridge.recipe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.recipe.dto.response.RecipeListResponse;

@Service
public class RecipeService {

	public List<RecipeListResponse> getRecipes() {
        return List.of(
            RecipeListResponse.builder()
                .recipeId(1L)
                .title("토마토 파스타")
                .summary("간단 파스타")
                .cookTime("20분")
                .build()
        );
    }
}
