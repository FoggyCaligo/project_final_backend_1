package com.today.fridge.chatbot.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.service.RecipeEmbeddingSearchService;
import com.today.fridge.llm.service.RecommendationExplanationService;
import com.today.fridge.support.IntegrationTestSupport;
import static org.mockito.ArgumentMatchers.anyInt;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class ChatbotControllerIT extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbeddingClient embeddingClient;

    @MockBean
    private RecommendationExplanationService recommendationExplanationService;

    @MockBean
    private RecipeEmbeddingSearchService recipeEmbeddingSearchService;
    @Test
    @DisplayName("챗봇 추천 API 통합 테스트")
    void recommend_success() throws Exception {

        // embedding mock
        org.mockito.BDDMockito.given(
                embeddingClient.generateEmbedding(
                        org.mockito.ArgumentMatchers.anyString()
                )
        ).willReturn(
                java.util.List.of(0.1, 0.2, 0.3)
        );

        // llm mock
        org.mockito.BDDMockito.given(
                recommendationExplanationService.generateExplanation(
                        org.mockito.ArgumentMatchers.any()
                )
        ).willReturn("저염식 추천입니다.");
        
        org.mockito.BDDMockito.given(
        	    recipeEmbeddingSearchService.searchSimilarRecipes(
        	            anyString(),
        	            anyInt()
        	    )
        	).willReturn(List.of());

        String requestJson = """
                {
                  "userId": 1,
                  "text": "저염식 국 추천해줘"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/chat/recommend")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("챗봇 기반 추천 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].recipeId").exists())
                .andExpect(jsonPath("$.data[0].title").exists())
                .andExpect(jsonPath("$.data[0].matchRate").exists())
                .andExpect(jsonPath("$.data[0].totalScore").exists())
                .andExpect(jsonPath("$.data[0].llmExplanation").exists());
    }
}