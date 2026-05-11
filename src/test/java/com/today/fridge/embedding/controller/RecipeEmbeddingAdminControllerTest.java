package com.today.fridge.embedding.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.today.fridge.embedding.service.RecipeEmbeddingBulkService;

@WebMvcTest(RecipeEmbeddingAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeEmbeddingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeEmbeddingBulkService recipeEmbeddingBulkService;

    @Test
    @DisplayName("임베딩이 없는 레시피의 임베딩을 벌크 생성한다")
    void generateMissingRecipeEmbeddings_success() throws Exception {

        when(recipeEmbeddingBulkService.generateMissingEmbeddings())
                .thenReturn(3);

        mockMvc.perform(
                        post("/api/v1/admin/embeddings/recipes/missing")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("generated embeddings: 3"));

        verify(recipeEmbeddingBulkService).generateMissingEmbeddings();
    }
}