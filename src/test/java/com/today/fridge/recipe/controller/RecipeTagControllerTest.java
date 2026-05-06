package com.today.fridge.recipe.controller;

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

import com.today.fridge.recipe.service.RecipeTagBulkService;

@WebMvcTest(RecipeTagController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeTagBulkService recipeTagBulkService;

    @Test
    @DisplayName("레시피 태그를 벌크 생성한다")
    void generateTags_success() throws Exception {

        when(recipeTagBulkService.generateMissingTags())
                .thenReturn(5);

        mockMvc.perform(
                        post("/api/v1/recipe-tags/generate")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("생성된 태그 수: 5"));

        verify(recipeTagBulkService).generateMissingTags();
    }
}