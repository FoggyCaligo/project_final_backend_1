package com.today.fridge.embedding.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;
import com.today.fridge.embedding.util.VectorUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeEmbeddingSearchService {

    private final EmbeddingClient embeddingClient;
    private final RecipeEmbeddingRepository recipeEmbeddingRepository;

    public List<Long> searchSimilarRecipeIds(
            String queryText,
            int limit
    ) {

        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }

        if (limit <= 0) {
            limit = 10;
        }

        // 1. FastAPI 임베딩 호출
        List<Double> vector =
                embeddingClient.generateEmbedding(queryText);

        // 2. pgvector literal 변환
        String queryVector =
                VectorUtils.toVectorLiteral(vector);

        // 3. similarity search
        return recipeEmbeddingRepository
                .findSimilarRecipeIds(
                        queryVector,
                        limit
                );
    }
  }