package com.today.fridge.ingredient.controller;

import com.today.fridge.global.config.SecurityConfig;
import com.today.fridge.global.exception.GlobalExceptionHandler;
import com.today.fridge.ingredient.service.UserIngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FridgeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class FridgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIngredientService userIngredientService;

    @Test
    void listWithoutUserId_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/fridge/ingredients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void summaryWithoutUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/fridge/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createWithoutUserId_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/fridge/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"상추\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
