package com.today.fridge.embedding.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.embedding.service.RecipeEmbeddingBulkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/embeddings")
public class RecipeEmbeddingAdminController {

    private final RecipeEmbeddingBulkService recipeEmbeddingBulkService;

    @PostMapping("/recipes/missing")
    public String generateMissingRecipeEmbeddings() {

        int count =
                recipeEmbeddingBulkService.generateMissingEmbeddings();

        return "generated embeddings: " + count;
    }
}