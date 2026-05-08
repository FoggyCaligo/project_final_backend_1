package com.today.fridge.recipe.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.recipe.service.RecipeTagBulkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recipe-tags")
public class RecipeTagController {

    private final RecipeTagBulkService recipeTagBulkService;

    @PostMapping("/generate")
    public String generateTags() {
        log.info("[RecipeTagController] generateTags (public)");

        int count = recipeTagBulkService.generateMissingTags();

        return "생성된 태그 수: " + count;
    }
}