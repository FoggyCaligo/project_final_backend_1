package com.today.fridge.recipe.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.service.RecipeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {
	
	private final RecipeService recipeService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResult<RecipeListResponse>>> getRecipes(
	        @PageableDefault(size = 12)
	        Pageable pageable
	) {

	    return ResponseEntity.ok(
	            ApiResponse.success(
	                    recipeService.getRecipes(pageable),
	                    "전체 레시피 조회 성공"
	            )
	    );
	}
}
