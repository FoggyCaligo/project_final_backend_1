package com.today.fridge.recommendation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.service.RecipeEmbeddingSearchService;
import com.today.fridge.llm.service.RecommendationExplanationService;
import com.today.fridge.support.IntegrationTestSupport;

class RecommendationControllerIT extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbeddingClient embeddingClient;

    @MockBean
    private RecipeEmbeddingSearchService recipeEmbeddingSearchService;

    @MockBean
    private RecommendationExplanationService recommendationExplanationService;

    @Test
    @DisplayName("추천 레시피 목록 조회 통합 테스트")
    void recommend_success() throws Exception {

        // embedding mock
        given(
                embeddingClient.generateEmbedding(anyString())
        ).willReturn(
                List.of(0.1, 0.2, 0.3)
        );

        // semantic search mock
        given(
                recipeEmbeddingSearchService.searchSimilarRecipes(
                        anyString(),
                        anyInt()
                )
        ).willReturn(List.of());

        // llm mock
        given(
                recommendationExplanationService.generateExplanation(
                        any()
                )
        ).willReturn("추천 이유입니다.");

        mockMvc.perform(
                        get("/api/v1/recipes/recommendations")
                                .param("page", "0")
                                .param("size", "9")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("추천 레시피 조회 성"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}