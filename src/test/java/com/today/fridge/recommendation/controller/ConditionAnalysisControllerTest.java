package com.today.fridge.recommendation.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.today.fridge.recommendation.service.RecipeConditionAnalyzeService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ConditionAnalysisController.class)
class ConditionAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeConditionAnalyzeService recipeConditionAnalyzeService;

    @Test
    @DisplayName("단일 레시피 조건 분석을 실행한다")
    void analyze_success() throws Exception {

        mockMvc.perform(
                        post("/api/v1/admin/recommendation/recipes/{recipeId}/condition-analysis", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("임베딩 기반 조건 분석 저장 완료"));

        verify(recipeConditionAnalyzeService).analyzeAndSave(1L);
    }

    @Test
    @DisplayName("전체 레시피 조건 분석을 실행한다")
    void analyzeAll_success() throws Exception {

        mockMvc.perform(
                        post("/api/v1/admin/recommendation/recipes/condition-analysis/bulk")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("전체 레시피 조건 분석 완료"));

        verify(recipeConditionAnalyzeService).analyzeAllRecipes();
    }
}