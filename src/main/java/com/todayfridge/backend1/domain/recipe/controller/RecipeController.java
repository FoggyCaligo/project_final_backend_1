package com.todayfridge.backend1.domain.recipe.controller;

import com.todayfridge.backend1.domain.recipe.service.RecipeQueryService;
import com.todayfridge.backend1.global.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {
    private final RecipeQueryService recipeQueryService;

    public RecipeController(RecipeQueryService recipeQueryService) {
        this.recipeQueryService = recipeQueryService;
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(recipeQueryService.search(keyword));
    }

    @GetMapping("/{recipeId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long recipeId) {
        return ApiResponse.success(recipeQueryService.detail(recipeId));
    }
}
