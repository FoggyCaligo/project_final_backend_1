package com.today.fridge.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.recipe.service.RecipeTagBulkService;

import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "RecipeTag", description = "RecipeTagController API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/recipe-tags")
public class RecipeTagController {

    private final RecipeTagBulkService recipeTagBulkService;

    @PostMapping("/generate")
    @Operation(summary = "RecipeTag API")
    public String generateTags() {

        int count = recipeTagBulkService.generateMissingTags();

        return "생성된 태그 수: " + count;
    }
}