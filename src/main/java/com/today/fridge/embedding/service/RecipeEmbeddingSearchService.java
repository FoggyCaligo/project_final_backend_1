package com.today.fridge.embedding.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeEmbeddingSearchService {

    private final RecipeEmbeddingRepository recipeEmbeddingRepository;

    public List<Long> searchSimilarRecipeIds(String queryVector, int limit) {
        if (queryVector == null || queryVector.isBlank()) {
            return List.of();
        }

        if (limit <= 0) {
            limit = 20;
        }

        return recipeEmbeddingRepository.findSimilarRecipeIds(queryVector, limit);
    }
}